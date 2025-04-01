package io.slingr.service.ethereum;

import com.google.common.collect.EvictingQueue;
import io.slingr.services.services.AppLogs;
import io.slingr.services.services.datastores.DataStore;
import io.slingr.services.services.datastores.DataStoreResponse;
import io.slingr.services.utils.Json;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BlocksManager {
    private static final Logger logger = LoggerFactory.getLogger(BlocksManager.class);

    private final int MAX_BLOCKS = 12;
    private final int MAX_BLOCKS_DELAY = 360;
    private final int CLEANING_WAIT_TIME_MINUTES = 5;
    private final int MAX_SHUTDOWN_MINUTES = 5;

    private EthereumApiHelper ethereumApiHelper;
    private AppLogs appLogger;
    private DataStore blocksDs;
    private EthereumEvent callbacks;
    private final Json config;

    private String lastProcessedBlockHash = null;
    private EvictingQueue<String> lastBlocksHashes = EvictingQueue.create(MAX_BLOCKS);
    private ScheduledExecutorService blockPollingExecutor;
    private ScheduledExecutorService cleanerExecutor;
    private Lock lock = new ReentrantLock();
    private long pollingWaitTime;

    public BlocksManager(EthereumApiHelper ethereumApiHelper, AppLogs appLogger, DataStore blocksDs, EthereumEvent callbacks, Json config) {
        this.ethereumApiHelper = ethereumApiHelper;
        this.callbacks = callbacks;
        this.appLogger = appLogger;
        this.blocksDs = blocksDs;
        this.config = config;
        this.pollingWaitTime = this.config.longInteger("pollingInterval", 5000);
    }

    public void start() {
        initLastBlocks();
        // execute thread to check new blocks periodically
        blockPollingExecutor = Executors.newSingleThreadScheduledExecutor();
        Runnable blockPollingTask = () -> {
            lock.lock();
            try {
                List<Json> newBlocks = new ArrayList<>();
                //appLogger.info("Getting last blocks from ethereum network");
                Json lastBlock = getLastBlock();
                if (lastBlock == null || StringUtils.equals(lastBlock.string("hash"), lastProcessedBlockHash)) {
                    // we don't have anything to update
                    return;
                }
                newBlocks.add(lastBlock);
                String parentHash = lastBlock.string("parentHash");
                // it can happen that the endpoint gets behind, and we need to process many blocks at the same time
                // we will do this for a maximum of 360 blocks, which would be like an hour
                while (!lastBlocksHashes.contains(parentHash) && newBlocks.size() < MAX_BLOCKS_DELAY) {
                    appLogger.info(String.format("Getting block [%s] from ethereum network", parentHash));
                    lastBlock = getBlockByHash(parentHash);
                    if (lastBlock == null) {
                        appLogger.warn("There were some issues polling for new blocks. We will retry in [" + pollingWaitTime + "] milliseconds");
                        return;
                    }
                    newBlocks.add(lastBlock);
                    parentHash = lastBlock.string("parentHash");
                }
                Collections.reverse(newBlocks);
                for (Json newBlock : newBlocks) {
                    processNewBlock(newBlock);
                }
            } catch (Exception e) {
                appLogger.error(String.format("Error polling for new blocks: [%s] consider to decrease the pollingWaitTime: [%d]", e.getMessage(), pollingWaitTime));
                logger.error(String.format("Error polling for new blocks: [%s]", e.getMessage()), e);
            } finally {
                lock.unlock();
            }
        };
        blockPollingExecutor.scheduleAtFixedRate(blockPollingTask, pollingWaitTime, pollingWaitTime, TimeUnit.MILLISECONDS);
        // execute thread to clean blocks from the database
        cleanerExecutor = Executors.newSingleThreadScheduledExecutor();
        Runnable cleanerTask = () -> {
            lock.lock();
            try {
                DataStoreResponse res = blocksDs.find();
                List<Json> blocks = res.items();
                int amountToRemove = blocks.size() - (MAX_BLOCKS * 2);
                if (amountToRemove > 0) {
                    appLogger.info(String.format("Removing [%s] old blocks", amountToRemove));
                    for (int i = 0; i < amountToRemove; i++) {
                        Json block = blocks.get(i);
                        blocksDs.removeById(block.string("_id"));
                    }
                } else {
                    appLogger.info("There are no blocks to remove");
                }
            } catch (Exception e) {
                appLogger.error("Error cleaning old blocks", e);
            } finally {
                lock.unlock();
            }
        };
        cleanerExecutor.scheduleAtFixedRate(cleanerTask, CLEANING_WAIT_TIME_MINUTES, CLEANING_WAIT_TIME_MINUTES, TimeUnit.MINUTES);
    }

    public void shutdown() {
        blockPollingExecutor.shutdown();
        cleanerExecutor.shutdown();
        try {
            blockPollingExecutor.awaitTermination(MAX_SHUTDOWN_MINUTES, TimeUnit.MINUTES);
            cleanerExecutor.awaitTermination(MAX_SHUTDOWN_MINUTES, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            appLogger.error("Error shutting down blocks manager");
        }
    }

    public void processNewBlock(Json newBlockJson) {
        logger.debug(String.format("New block arrived with hash [%s]", newBlockJson.string("hash")));
        Block newBlock = new Block();
        newBlock.fromGethJson(newBlockJson);
        // check if the parent doesn't match with the last block, which means a chain reorganization happened
        // we also check that the parent is in the last blocks, otherwise it means we just got behind for too long
        if (lastProcessedBlockHash != null && !StringUtils.equals(newBlock.getParentHash(), lastProcessedBlockHash) && lastBlocksHashes.contains(newBlock.getParentHash())) {
            reorganizeChain(newBlock);
        }
        blocksDs.save(newBlock.toJson());
        lastProcessedBlockHash = newBlock.getHash();
        lastBlocksHashes.add(lastProcessedBlockHash);
        callbacks.onNewBlock(newBlock);
    }

    private void reorganizeChain(Block newBlock) {
        List<Json> blocks = getLastBlocksInDs();
        for (int i = 0; i < blocks.size(); i++) {
            Json block = blocks.get(i);
            if (StringUtils.equals(block.string(Block.HASH), newBlock.getParentHash())) {
                // we found the common parent, so we stop at this point
                break;
            }
            block.set(Block.REMOVED, "true");
            blocksDs.update(block);
            Block removedBlock = new Block();
            removedBlock.fromJson(block);
            callbacks.onRemovedBlock(removedBlock);
        }
        initLastBlocks();
    }

    private void initLastBlocks() {
        lastProcessedBlockHash = null;
        lastBlocksHashes.clear();
        List<Json> blocks = getLastBlocksInDs();
        if (blocks.isEmpty()) {
            Json lastBlock = getLastBlock();
            blocks.add(lastBlock);
            while (blocks.size() < MAX_BLOCKS) {
                lastBlock = getBlockByHash(lastBlock.string("parentHash"));
                blocks.add(lastBlock);
            }
        }
        Collections.reverse(blocks);
        for (Json block : blocks) {
            lastProcessedBlockHash = block.string(Block.HASH);
            lastBlocksHashes.add(lastProcessedBlockHash);
        }
    }

    private List<Json> getLastBlocksInDs() {
        Json filter = Json.map().set(Block.REMOVED, "false");
        DataStoreResponse res = blocksDs.find(filter);
        List<Json> blocks = res.items();
        // from newer to older
        Collections.reverse(blocks);
        // just keep the last blocks in memory
        while (blocks.size() > MAX_BLOCKS) {
            blocks.remove(MAX_BLOCKS);
        }
        return blocks;
    }

    private Json getBlockByHash(String hash) {
        return ethereumApiHelper.getBlockByHash(hash, false);
    }

    private Json getLastBlock() {
        return ethereumApiHelper.getBlockByNumber("latest", false);
    }

}

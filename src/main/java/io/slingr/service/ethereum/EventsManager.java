package io.slingr.service.ethereum;

import com.google.common.collect.EvictingQueue;
import io.slingr.services.services.AppLogs;
import io.slingr.services.services.Events;
import io.slingr.services.services.datastores.DataStore;
import io.slingr.services.services.datastores.DataStoreResponse;
import io.slingr.services.utils.Json;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class EventsManager {
    private static final int MAX_BLOCKS = 12;
    private final int CLEANING_WAIT_TIME_MINUTES = 5;
    private final int MAX_SHUTDOWN_MINUTES = 5;

    private final String EVENT_CONTRACT_EVENT = "contractEvent";

    private final String EVENTS_BLOCK_HASH = "blockHash";
    private final String EVENTS_BLOCK = "block";
    private final String EVENTS_LOGS = "logs";
    private final String EVENTS_REMOVED = "removed";
    private final String EVENTS_SENT = "sent";

    private final String APP_EVENTS_RAW_EVENT = "rawEvent";
    private final String APP_EVENTS_EVENT_NAME = "eventName";
    private final String APP_EVENTS_EVENT_DATA = "eventData";

    private EthereumHelper ethereumHelper = new EthereumHelper();
    private EthereumApiHelper ethereumApiHelper;
    private Events events;
    private AppLogs appLogger;
    private DataStore eventsDs;
    private DataStore contractsDs;
    private List<Json> configuredContracts;
    private long confirmationBlocks;
    private final Json config;

    private EvictingQueue<Json> pendingEvents = EvictingQueue.create(MAX_BLOCKS);
    private Map<String, Map> contractsEvents = new HashMap<>();
    private ScheduledExecutorService cleanerExecutor;
    private Lock lock = new ReentrantLock();

    public EventsManager(EthereumApiHelper ethereumApiHelper, Events events, AppLogs appLogger, DataStore eventsDs, DataStore contractsDs, List<Json> configuredContracts, long confirmationBlocks, Json config) {
        this.ethereumApiHelper = ethereumApiHelper;
        this.appLogger = appLogger;
        this.events = events;
        this.eventsDs = eventsDs;
        this.contractsDs = contractsDs;
        this.configuredContracts = configuredContracts;
        this.confirmationBlocks = confirmationBlocks;
        this.config = config;
    }

    public void start() {
        appLogger.info("Loading contracts' topics...");
        if (configuredContracts != null) {
            for (Json co : configuredContracts) {
                appLogger.info("Subscribing to events in contract " + co.string("address"));
                registerContract(co.string("address").toLowerCase(), co.json("abi"));
            }
        }
        appLogger.info("Done loading contracts' topics");
        DataStoreResponse dynamicContracts = contractsDs.find();
        appLogger.info("Loading dynamic contracts' topics...");
        for (Json co : dynamicContracts.items()) {
            appLogger.info("Subscribing to events in contract " + co.string("address"));
            registerContract(co.string("address"), co.json("abi"));
        }
        appLogger.info("Done loading dynamic contracts' topics");
        // load events from database that have not been removed or sent already
        DataStoreResponse eventsRes = eventsDs.find(Json.map().set("removed", "false").set("sent", "false"));
        for (Json json : eventsRes.items()) {
            pendingEvents.add(json);
        }
        // execute thread to clean blocks from the database
        cleanerExecutor = Executors.newSingleThreadScheduledExecutor();
        Runnable cleanerTask = () -> {
            lock.lock();
            try {
                DataStoreResponse res = eventsDs.find();
                List<Json> blocks = res.items();
                int amountToRemove = blocks.size() - (MAX_BLOCKS * 10);
                if (amountToRemove > 0) {
                    appLogger.info(String.format("Removing [%s] old block events", amountToRemove));
                    for (int i = 0; i < amountToRemove; i++) {
                        Json block = blocks.get(i);
                        eventsDs.removeById(block.string("_id"));
                    }
                } else {
                    appLogger.info("There are no block logs to remove");
                }
            } catch (Exception e) {
                appLogger.error("Error cleaning old block logs", e);
            } finally {
                lock.unlock();
            }
        };
        cleanerExecutor.scheduleAtFixedRate(cleanerTask, CLEANING_WAIT_TIME_MINUTES, CLEANING_WAIT_TIME_MINUTES, TimeUnit.MINUTES);
    }

    public void shutdown() {
        cleanerExecutor.shutdown();
        try {
            cleanerExecutor.awaitTermination(MAX_SHUTDOWN_MINUTES, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            appLogger.error("Error shutting down blocks manager");
        }
    }

    public void processEventsInBlock(Block block) {
        lock.lock();
        try {
            List<Json> logs = getLogsInBlock(block);
            addBlockLogs(block, logs);
            checkLogsToSend(block);
        } finally {
            lock.unlock();
        }
    }

    public void removeEventsInBlock(Block block) {
        lock.lock();
        try {
            // get logs for removed block from database
            Json blockEvents = eventsDs.findOne(Json.map().set(EVENTS_BLOCK_HASH, block.getHash()));
            if (blockEvents != null && !blockEvents.isEmpty(EVENTS_BLOCK_HASH)) {
                if ("true".equals(blockEvents.string(EVENTS_SENT))) {
                    // if they were sent, send the removed event
                    sendEvents(blockEvents, true);
                }
                // remove them from queue and mark as removed in database
                pendingEvents.removeIf(json -> {
                    if (StringUtils.equals(block.getHash(), json.string(EVENTS_BLOCK_HASH))) {
                        return true;
                    }
                    return false;
                });
                blockEvents.set(EVENTS_REMOVED, "true");
                eventsDs.update(blockEvents);
            }
        } finally {
            lock.unlock();
        }
    }

    public void registerContract(String address, Json abiDefinition) {
        lock.lock();
        try {
            Map<String, Json> topics = new HashMap<>();
            boolean added = false;
            for (Json abi : abiDefinition.jsons()) {
                if ("event".equals(abi.string("type"))) {
                    added = true;
                    String topic = "0x" + ethereumHelper.encodeEvent(abi);
                    topics.put(topic, abi);
                }
            }
            if (added) {
                contractsEvents.put(address, topics);
            }
        } finally {
            lock.unlock();
        }
    }

    public void removeContract(String address) {
        lock.lock();
        try {
            contractsEvents.remove(address);
        } finally {
            lock.unlock();
        }
    }

    public Json decodeEvent(String address, List<Object> topicsList, String data) {
        Json result = null;
        Map<String, Json> topics = contractsEvents.get(address);
        if (topics != null && topicsList != null) {
            if (topicsList.size() > 0) {
                Json abi = topics.get(topicsList.get(0));
                if (abi != null) {
                    result = ethereumHelper.processResult(abi, topicsList, data);
                }
            }
        }
        return result;
    }

    private List<Json> getLogsInBlock(Block block) {
        List<Json> logs = ethereumApiHelper.getLogsByBlock(block.getHash());
        logs.removeIf(log -> {
            String contractAddress = log.string("address");
            if (!contractsEvents.containsKey(contractAddress)) {
                return true;
            }
            return false;
        });
        return logs;
    }

    private void addBlockLogs(Block block, List<Json> logs) {
        Json eventsDocument = Json.map();
        eventsDocument.set(EVENTS_BLOCK_HASH, block.getHash());
        eventsDocument.set(EVENTS_BLOCK, block.toJson());
        eventsDocument.set(EVENTS_LOGS, logs);
        eventsDocument.set(EVENTS_REMOVED, "false");
        eventsDocument.set(EVENTS_SENT, "false");
        eventsDs.save(eventsDocument);
        pendingEvents.add(eventsDocument);
    }

    private void checkLogsToSend(Block block) {
        long diff;
        do {
            Json events = pendingEvents.peek();
            long blockNumber = Long.parseLong(events.json(EVENTS_BLOCK).string(Block.NUMBER));
            diff = block.getNumber() - blockNumber;
            if (diff >= confirmationBlocks) {
                sendEvents(events, false);
                events.string(EVENTS_SENT, "true");
                try {
                    eventsDs.update(events);
                } catch (Exception e) {
                    appLogger.error(String.format("There were errors trying to update block events with hash [%s] as sent", events.string(EVENTS_BLOCK_HASH)), e);
                }
                pendingEvents.poll();
            }
        } while (!pendingEvents.isEmpty() && diff > confirmationBlocks);
    }

    private void sendEvents(Json logsDb, boolean removed) {
        List<Json> logs = logsDb.jsons(EVENTS_LOGS);
        for (Json log : logs) {
            Json parsedLog = decodeEvent(log.string("address"), log.objects("topics"), log.string("data"));
            if (parsedLog != null) {
                Json event = Json.map();
                log.set("removed", removed);
                event.set(APP_EVENTS_RAW_EVENT, log);
                event.set(APP_EVENTS_EVENT_NAME, parsedLog.string("eventName"));
                event.set(APP_EVENTS_EVENT_DATA, parsedLog.json("eventData"));
                // event should be sent to the proper apps
                if (isSharedEndpoint()) {
                    events.send(EVENT_CONTRACT_EVENT, logsDb.string("app"), logsDb.string("env"), event);
                } else {
                    events.send(EVENT_CONTRACT_EVENT, event);
                }
            }
        }
    }

    private boolean isSharedEndpoint() {
        return config != null && config.bool("shared", false);
    }
}

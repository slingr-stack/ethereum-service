package io.slingr.service.ethereum;

import io.slingr.services.services.AppLogs;
import io.slingr.services.services.Events;
import io.slingr.services.services.datastores.DataStore;
import io.slingr.services.services.datastores.DataStoreResponse;
import io.slingr.services.utils.Json;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TransactionManager {
    private final int CLEANING_WAIT_TIME_MINUTES = 5;
    private final int MAX_SHUTDOWN_MINUTES = 5;
    private final int TRANSACTION_LIFE = 60 * 60 * 1000; // one hour

    private final String EVENT_TX_CONFIRMED = "transactionConfirmed";
    private final String EVENT_TX_REJECTED = "transactionRejected";
    private final String EVENT_TX_REMOVED = "transactionRemoved";
    private final String FROM = "from";

    private EthereumApiHelper ethereumApiHelper;
    private Events events;
    private AppLogs appLogger;
    private DataStore transactionsDs;
    private final Json config;

    private Map<String, Transaction> pendingTransactions = new LinkedHashMap<>();
    private ScheduledExecutorService cleanerExecutor;
    private Lock lock = new ReentrantLock();

    public TransactionManager(EthereumApiHelper ethereumApiHelper, Events events, AppLogs appLogger, DataStore transactionsDs, Json config) {
        this.ethereumApiHelper = ethereumApiHelper;
        this.events = events;
        this.appLogger = appLogger;
        this.transactionsDs = transactionsDs;
        this.config = config;
    }

    public void start() {
        // load pending transactions from database
        appLogger.info("Loading pending transactions from database");
        DataStoreResponse txsRes = transactionsDs.find(null, null, 1000);
        int pendingTxsCount = 0;
        while (txsRes.items().size() > 0) {
            List<Json> txs = txsRes.items();
            for (Json tx : txs) {
                if (Transaction.STATUS_PENDING.equals(tx.string(Transaction.STATUS))
                        || Transaction.STATUS_CONFIRMED.equals(tx.string(Transaction.STATUS))
                ) {
                    Transaction txObj = new Transaction(tx);
                    pendingTransactions.put(txObj.getTxHash(), txObj);
                    pendingTxsCount++;
                }
            }
            txsRes = transactionsDs.find(null, txsRes.offset(), 1000);
        }
        appLogger.info(String.format("[%s] transactions were loaded", pendingTxsCount));
        // execute thread to clean blocks from the database
        cleanerExecutor = Executors.newSingleThreadScheduledExecutor();
        Runnable cleanerTask = () -> {
            lock.lock();
            try {
                DataStoreResponse res = transactionsDs.find(null, null, 100);
                List<Json> txs = res.items();
                int count = 0;
                for (Json tx : txs) {
                    if (Transaction.STATUS_REMOVED.equals(tx.string(Transaction.STATUS))
                            || Transaction.STATUS_SENT.equals(tx.string(Transaction.STATUS))
                            || Transaction.STATUS_TIMEOUT.equals(tx.string(Transaction.STATUS))
                            || Transaction.STATUS_REPLACED.equals(tx.string(Transaction.STATUS))) {
                        long diff = new Date().getTime() - tx.longInteger(Transaction.TIMESTAMP);
                        if (diff > TRANSACTION_LIFE) {
                            transactionsDs.removeById(tx.string(Transaction.ID));
                            count++;
                        }
                    }
                }
                if (count > 0) {
                    appLogger.info(String.format("[%s] old transactions were removed", count));
                } else {
                    appLogger.info("There are no old transactions to remove");
                }
            } catch (Exception e) {
                appLogger.error("Error cleaning old transactions", e);
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
            appLogger.error("Error shutting down transaction manager");
        }
    }

    public void processTransactionsInBlock(Block block) {
        lock.lock();
        try {
            List<String> transactionInBlock = getTransactionsInBlock(block);
            // mark transactions in the new block as confirmed
            for (String txHash : transactionInBlock) {
                if (pendingTransactions.containsKey(txHash)) {
                    Transaction pendingTransaction = pendingTransactions.get(txHash);
                    pendingTransaction.setBlockHash(block.getHash());
                    pendingTransaction.setBlockNumber(block.getNumber());
                    pendingTransaction.setStatus(Transaction.STATUS_CONFIRMED);
                    transactionsDs.update(pendingTransaction.toJson());
                }
            }
            // check which ones have to be sent and which ones have timed out
            List<String> txsToRemove = new ArrayList<>();

            for (String txHash : pendingTransactions.keySet()) {
                Transaction tx = pendingTransactions.get(txHash);
                long diff = block.getNumber() - tx.getBlockNumber();
                if (Transaction.STATUS_CONFIRMED.equals(tx.getStatus()) && diff >= tx.getConfirmationBlocks()) {
                    Json receipt = ethereumApiHelper.getTransactionReceipt(txHash);
                    if (receipt == null) {
                        // sometimes, for some reason, the tx has the status confirmed but then when we look for the
                        // receipt, it isn't there; we need to keep checking for this transaction in those cases
                        continue;
                    }
                    sendEvent(EVENT_TX_CONFIRMED, tx, receipt);
                    tx.setStatus(Transaction.STATUS_SENT);
                    tx.setReceipt(receipt);
                    transactionsDs.update(tx.toJson());
                    txsToRemove.add(txHash);

                    for (String txHashReplaced : pendingTransactions.keySet()) {
                        Transaction txReplaced = pendingTransactions.get(txHashReplaced);
                        if (tx.getFrom().equals(txReplaced.getFrom()) && !txHashReplaced.equals(txHash) && tx.getNonce().equals(txReplaced.getNonce())) {
                            // check if there is a tx with the same nonce that was replaced
                            Json res = Json.map();
                            res.set("receipt", txReplaced.getReceipt());
                            res.set("errorCode", "replaced");
                            res.set("errorMessage", String.format("Transaction was replaced with tx [%s]", txHash));
                            sendEvent(EVENT_TX_REJECTED, txReplaced, res);
                            txReplaced.setStatus(Transaction.STATUS_REPLACED);
                            transactionsDs.update(txReplaced.toJson());
                            txsToRemove.add(txHashReplaced);
                        } else if (tx.getFrom().equals(txReplaced.getFrom()) && Integer.decode(txReplaced.getNonce()) < (Integer.decode(tx.getNonce()))) {
                            // check if there is a tx with a lower nonce that will never be mined
                            txReplaced.setStatus(Transaction.STATUS_REMOVED);
                            transactionsDs.update(txReplaced.toJson());
                            txsToRemove.add(txHashReplaced);
                        }
                    }
                } else if (Transaction.STATUS_PENDING.equals(tx.getStatus())) {
                    // check if the transaction has timed out
                    if (tx.getTimeout() < new Date().getTime()) {
                        Json res = Json.map();
                        res.set("receipt", tx.getReceipt());
                        res.set("errorCode", "timeout");
                        long minAsMil = tx.getTimeout() + (1000 * 20) - tx.getTimestamp(); // plus 20 secs in order to round in minutes.
                        res.set("error", String.format("Transaction could not be confirmed after %s minutes", TimeUnit.MILLISECONDS.toMinutes(minAsMil)));
                        sendEvent(EVENT_TX_REJECTED, tx, res);
                        tx.setStatus(Transaction.STATUS_TIMEOUT);
                        transactionsDs.update(tx.toJson());
                        txsToRemove.add(txHash);
                    }
                }
            }
            // clean up sent transactions from memory
            for (String txHash : txsToRemove) {
                pendingTransactions.remove(txHash);
            }
        } finally {
            lock.unlock();
        }
    }

    private void sendEvent(String event, Transaction transaction, Json res) {
        sendEvent(event, transaction, res, transaction.getFunctionId());
    }

    private void sendEvent(String event, Transaction transaction, Json res, String functionId) {
        if (isSharedEndpoint()) {
            events.send(event, res, functionId);
        } else {
            events.send(event, res, functionId);
        }
    }

    public void removeTransactionsInBlock(Block block) {
        lock.lock();
        try {
            DataStoreResponse res = transactionsDs.find(Json.map().set(Transaction.BLOCK_HASH, block.getHash()));
            List<Json> transactionsToRemove = res.items();
            for (Json transactionJson : transactionsToRemove) {
                Transaction transaction = new Transaction(transactionJson);
                if (Transaction.STATUS_CONFIRMED.equals(transaction.getStatus())) {
                    transaction.setReceipt(null);
                    transaction.setBlockHash(null);
                    transaction.setBlockNumber(0);
                    transaction.setStatus(Transaction.STATUS_PENDING);
                    transactionsDs.update(transaction.toJson());
                    pendingTransactions.put(transaction.getTxHash(), transaction);
                } else if (Transaction.STATUS_SENT.equals(transaction.getStatus())) {
                    sendEvent(EVENT_TX_REMOVED, transaction, transaction.getReceipt(), null);
                    transaction.setStatus(Transaction.STATUS_REMOVED);
                    transactionsDs.update(transaction.toJson());
                    pendingTransactions.remove(transaction.getTxHash());
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void registerTransaction(String txHash, String nonce, String from, String functionId, long timestamp, long confirmationTimeout, long confirmationBlocks) {
        lock.lock();
        try {
            Transaction tx = new Transaction(txHash, nonce, from, functionId, timestamp, confirmationTimeout, confirmationBlocks);
            Json txJson = transactionsDs.save(tx.toJson());
            tx.setId(txJson.string(Transaction.ID));
            pendingTransactions.put(txHash, tx);
        } finally {
            lock.unlock();
        }
    }

    private List<String> getTransactionsInBlock(Block block) {
        Json res = ethereumApiHelper.getBlockByHash(block.getHash(), false);
        List<String> transactions = res != null && res.strings("transactions") != null ? res.strings("transactions") : new ArrayList<>();
        return transactions;
    }

    private boolean isSharedEndpoint() {
        return config != null && config.bool("shared", false);
    }
}

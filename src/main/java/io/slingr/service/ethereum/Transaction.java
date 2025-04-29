package io.slingr.service.ethereum;

import io.slingr.services.utils.Json;

public class Transaction {

    public final static String ID = "_id";
    public final static String TX_HASH = "txHash";
    public final static String BLOCK_HASH = "blockHash";
    public final static String BLOCK_NUMBER = "blockNumber";
    public final static String FUNCTION_ID = "functionId";
    public final static String STATUS = "status";
    public final static String TIMESTAMP = "timestamp";
    public final static String TIMEOUT = "timeout";
    public final static String NONCE = "nonce";
    public final static String FROM = "from";
    public final static String CONFIRMATION_BLOCKS = "confirmationBlocks";
    public final static String RECEIPT = "receipt";
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_CONFIRMED = "confirmed";
    public static final String STATUS_REMOVED = "removed";
    public static final String STATUS_SENT = "sent";
    public static final String STATUS_TIMEOUT = "timeout";
    public static final String STATUS_REPLACED = "replaced";
    public static final String APP = "app";
    public static final String ENV = "env";

    private String id;
    private String txHash;
    private String blockHash;
    private long blockNumber;
    private String functionId;
    private long timestamp;
    private long timeout;
    private long confirmationBlocks;
    private String status = STATUS_PENDING;

    private String nonce;
    private String from;
    private String app;
    private String env;
    private Json receipt;

    public Transaction(String txHash, String nonce, String from, String functionId, long timestamp, long timeout, long confirmationBlocks) {
        this.txHash = txHash;
        this.nonce = nonce;
        this.from = from;
        this.functionId = functionId;
        this.timestamp = timestamp;
        this.timeout = timeout;
        this.confirmationBlocks = confirmationBlocks;
        this.receipt = null;
    }

    public Transaction(Json json) {
        this.fromJson(json);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash;
    }

    public long getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(long blockNumber) {
        this.blockNumber = blockNumber;
    }

    public String getFunctionId() {
        return functionId;
    }

    public void setFunctionId(String functionId) {
        this.functionId = functionId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public long getConfirmationBlocks() {
        return confirmationBlocks;
    }

    public void setConfirmationBlocks(long confirmationBlocks) {
        this.confirmationBlocks = confirmationBlocks;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Json getReceipt() {
        return receipt;
    }

    public void setReceipt(Json receipt) {
        this.receipt = receipt;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public Json toJson() {
        return Json.map()
                .set(ID, this.getId())
                .set(TX_HASH, this.getTxHash())
                .set(NONCE, this.getNonce())
                .set(FROM, this.getFrom())
                .set(BLOCK_HASH, this.getBlockHash())
                .set(BLOCK_NUMBER, this.getBlockNumber())
                .set(FUNCTION_ID, this.getFunctionId())
                .set(STATUS, this.getStatus())
                .set(CONFIRMATION_BLOCKS, this.getConfirmationBlocks())
                .set(TIMESTAMP, this.getTimestamp())
                .set(TIMEOUT, this.getTimeout())
                .set(RECEIPT, this.getReceipt());
    }

    public void fromJson(Json tx) {
        this.setId(tx.string(ID));
        this.setTxHash(tx.string(TX_HASH));
        this.setNonce(tx.string(NONCE));
        this.setFrom(tx.string(FROM));
        this.setBlockHash(tx.string(BLOCK_HASH));
        this.setBlockNumber(tx.longInteger(BLOCK_NUMBER));
        this.setFunctionId(tx.string(FUNCTION_ID));
        this.setStatus(tx.string(STATUS));
        this.setConfirmationBlocks(tx.integer(CONFIRMATION_BLOCKS));
        this.setTimestamp(tx.longInteger(TIMESTAMP));
        this.setTimeout(tx.longInteger(TIMEOUT));
        this.setReceipt(tx.json(RECEIPT));
    }
}

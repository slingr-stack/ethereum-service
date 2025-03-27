package io.slingr.endpoints.ethereum;

import io.slingr.endpoints.utils.Json;

public class Block {
    public static final String PARENT_HASH = "parentHash";
    public static final String HASH = "hash";
    public static final String NUMBER = "number";
    public static final String REMOVED = "removed";
    public static final String TIMESTAMP = "timestamp";
    public static final String ORIGINAL_BLOCK_INFO = "originalBlockInfo";


    private String parentHash;
    private String hash;
    private long number;
    private boolean removed;
    private long timestamp;
    private Json originalBlockInfo;

    public Block() {

    }

    public Block(String parentHash, String hash, long number, boolean removed, long timestamp) {
        this.parentHash = parentHash;
        this.hash = hash;
        this.number = number;
        this.removed = removed;
        this.timestamp = timestamp;
    }

    public Block(Json json) {
        fromJson(json);
    }

    public String getParentHash() {
        return parentHash;
    }

    public void setParentHash(String parentHash) {
        this.parentHash = parentHash;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    public boolean isRemoved() {
        return removed;
    }

    public void setRemoved(boolean removed) {
        this.removed = removed;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Json getOriginalBlockInfo() {
        return originalBlockInfo;
    }

    public void setOriginalBlockInfo(Json originalBlockInfo) {
        this.originalBlockInfo = originalBlockInfo;
    }

    public void fromJson(Json json) {
        setParentHash(json.string(PARENT_HASH));
        setHash(json.string(HASH));
        setNumber(Long.parseLong(json.string(NUMBER)));
        setRemoved(Boolean.parseBoolean(json.string(REMOVED)));
        setTimestamp(json.longInteger(TIMESTAMP));
        setOriginalBlockInfo(json.json(ORIGINAL_BLOCK_INFO));
    }

    public Json toJson() {
        return Json.map()
                .set(PARENT_HASH, parentHash)
                .set(HASH, hash)
                .set(NUMBER, ""+number)
                .set(REMOVED, ""+removed) // we store as string so we can filter this field
                .set(TIMESTAMP, timestamp)
                .set(ORIGINAL_BLOCK_INFO, originalBlockInfo);
    }

    public void fromGethJson(Json json) {
        setParentHash(json.string("parentHash"));
        setHash(json.string("hash"));
        setNumber(EthereumHelper.convertedHexToNumber(json.string("number")));
        setRemoved(false);
        setTimestamp(EthereumHelper.convertedHexToNumber(json.string("timestamp")));
        setOriginalBlockInfo(json);
    }
}

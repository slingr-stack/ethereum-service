package io.slingr.endpoints.ethereum;

public interface EthereumEvent {
    void onNewBlock(Block block);
    void onRemovedBlock(Block block);
}

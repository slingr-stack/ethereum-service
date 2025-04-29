package io.slingr.service.ethereum;

public interface EthereumEvent {
    void onNewBlock(Block block);
    void onRemovedBlock(Block block);
}

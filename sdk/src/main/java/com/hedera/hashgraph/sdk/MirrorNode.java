package com.hedera.hashgraph.sdk;

import org.threeten.bp.Duration;

import java.util.concurrent.ExecutorService;

class MirrorNode extends ManagedNode<MirrorNode, String> {
    protected final ManagedChannelWrapper channelWrapper;

    MirrorNode(ManagedNodeAddress address, ExecutorService executor) {
        super(executor);
        channelWrapper = new ManagedChannelWrapper(this, executor, address);
    }

    MirrorNode(String address, ExecutorService executor) {
        this(ManagedNodeAddress.fromString(address), executor);
    }

    private MirrorNode(MirrorNode node, ManagedNodeAddress address) {
        super(node);
        channelWrapper = new ManagedChannelWrapper(this, node.channelWrapper.executor, address);
    }

    MirrorNode toInsecure() {
        return new MirrorNode(this, channelWrapper.address.toInsecure());
    }

    MirrorNode toSecure() {
        return new MirrorNode(this, channelWrapper.address.toSecure());
    }

    @Override
    String getKey() {
        return channelWrapper.address.toString();
    }

    @Override
    void shutdownChannels() {
        channelWrapper.shutdown();
    }

    @Override
    void awaitChannelsTermination(Duration timeout) throws InterruptedException {
        channelWrapper.awaitTermination(timeout.getSeconds());
    }

    @Override
    ManagedChannelWrapper getChannelWrapperForExecute() {
        return channelWrapper;
    }


}

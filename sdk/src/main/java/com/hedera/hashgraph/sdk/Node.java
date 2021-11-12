package com.hedera.hashgraph.sdk;

import io.grpc.ChannelCredentials;
import io.grpc.TlsChannelCredentials;
import org.threeten.bp.Duration;
import org.threeten.bp.Instant;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

class Node extends ManagedNode<Node, AccountId> {
    private final AccountId accountId;

    @Nullable
    private NodeAddress addressBook;

    private boolean verifyCertificates;

    protected final List<ManagedChannelWrapper> channelWrappers;
    private int channelIndex = 0;

    Node(AccountId accountId, ExecutorService executor) {
        super(executor);

        this.accountId = accountId;
        channelWrappers = new ArrayList<>();
    }

    private Node(Node node, boolean makeSecure) {
        super(node);

        this.accountId = node.accountId;
        this.verifyCertificates = node.verifyCertificates;
        this.addressBook = node.addressBook;

        this.channelWrappers = new ArrayList<>(node.channelWrappers.size());
        for (var channel : node.channelWrappers) {
            this.channelWrappers.add(new ManagedChannelWrapper(
                this,
                channel.executor,
                makeSecure ? channel.address.toSecure() : channel.address.toInsecure())
            );
        }
    }

    @Override
    Node toInsecure() {
        return new Node(this, false);
    }

    @Override
    Node toSecure() {
        return new Node(this, true);
    }

    @Override
    AccountId getKey() {
        return accountId;
    }

    AccountId getAccountId() {
        return accountId;
    }

    NodeAddress getAddressBook() {
        return addressBook;
    }

    Node setAddressBook(@Nullable NodeAddress addressBook) {
        this.addressBook = addressBook;
        return this;
    }

    Node addAddress(String addressString) {
        channelWrappers.add(new ManagedChannelWrapper(
            this,
            executor,
            ManagedNodeAddress.fromString(addressString))
        );
        return this;
    }

    boolean isVerifyCertificates() {
        return verifyCertificates;
    }

    Node setVerifyCertificates(boolean verifyCertificates) {
        this.verifyCertificates = verifyCertificates;
        return this;
    }

    @Override
    ChannelCredentials getChannelCredentials() {
        return TlsChannelCredentials.newBuilder()
            .trustManager(new HederaTrustManager(addressBook == null ? null : addressBook.certHash, verifyCertificates))
            .build();
    }

    @Override
    void shutdownChannels() {
        for (var channel : channelWrappers) {
            channel.shutdown();
        }
    }

    @Override
    void awaitChannelsTermination(Duration timeout) throws InterruptedException {
        var stopAt = Instant.now().getEpochSecond() + timeout.getSeconds();
        for (var channel : channelWrappers) {
            channel.awaitTermination(stopAt - Instant.now().getEpochSecond());
        }
    }

    @Override
    ManagedChannelWrapper getChannelWrapperForExecute() {
        return channelWrappers.get(channelIndex);
    }

    @Override
    synchronized void increaseDelay() {
        super.increaseDelay();
        channelIndex = (channelIndex + 1) % channelWrappers.size();
    }

    @Override
    public String toString() {
        return accountId.toString();
    }
}

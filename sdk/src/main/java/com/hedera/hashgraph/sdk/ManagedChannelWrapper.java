package com.hedera.hashgraph.sdk;

import com.google.errorprone.annotations.Var;
import io.grpc.ConnectivityState;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import java8.util.Objects;
import java8.util.concurrent.CompletableFuture;
import org.threeten.bp.Duration;

import javax.annotation.Nullable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ManagedChannelWrapper {
    private static final int GET_STATE_INTERVAL_MILLIS = 50;
    private static final int GET_STATE_TIMEOUT_MILLIS = 10000;
    private static final int GET_STATE_MAX_ATTEMPTS = GET_STATE_TIMEOUT_MILLIS / GET_STATE_INTERVAL_MILLIS;

    private final ManagedNode owningNode;
    private boolean hasConnected = false;

    protected final ExecutorService executor;

    /**
     * Address of this node
     */
    protected final ManagedNodeAddress address;

    @Nullable
    protected ManagedChannel channel = null;

    ManagedChannelWrapper(ManagedNode owningNode, ExecutorService executor, ManagedNodeAddress address) {
        this.owningNode = owningNode;
        this.executor = executor;
        this.address = address;
    }

    /**
     * Get the address of this node
     *
     * @return
     */
    ManagedNodeAddress getAddress() {
        return address;
    }

    /**
     * Get the gRPC channel for this node
     *
     * @return
     */
    synchronized ManagedChannel getChannel() {
        owningNode.markUsed();

        if (channel != null) {
            return channel;
        }

        ManagedChannelBuilder<?> channelBuilder;

        if (address.isInProcess()) {
            channelBuilder = InProcessChannelBuilder.forName(Objects.requireNonNull(address.getName()));
        } else if (address.isTransportSecurity()) {
            channelBuilder = Grpc.newChannelBuilder(address.toString(), owningNode.getChannelCredentials()).overrideAuthority("127.0.0.1");
        } else {
            channelBuilder = ManagedChannelBuilder.forTarget(address.toString()).usePlaintext();
        }

        channel = channelBuilder
            .keepAliveTimeout(10, TimeUnit.SECONDS)
            .userAgent(getUserAgent())
            .executor(executor)
            .build();

        return channel;
    }

    boolean channelFailedToConnect() {
        if (hasConnected) {
            return false;
        }
        hasConnected = (getChannel().getState(true) == ConnectivityState.READY);
        try {
            for (@Var int i = 0; i < GET_STATE_MAX_ATTEMPTS && !hasConnected; i++) {
                TimeUnit.MILLISECONDS.sleep(GET_STATE_INTERVAL_MILLIS);
                hasConnected = (getChannel().getState(true) == ConnectivityState.READY);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return !hasConnected;
    }

    private CompletableFuture<Boolean> channelFailedToConnectAsync(int i, ConnectivityState state) {
        hasConnected = (state == ConnectivityState.READY);
        if (i >= GET_STATE_MAX_ATTEMPTS || hasConnected) {
            return CompletableFuture.completedFuture(!hasConnected);
        }
        return Delayer.delayFor(GET_STATE_INTERVAL_MILLIS, executor).thenCompose(ignored -> {
            return channelFailedToConnectAsync(i + 1, getChannel().getState(true));
        });
    }

    CompletableFuture<Boolean> channelFailedToConnectAsync() {
        if (hasConnected) {
            return CompletableFuture.completedFuture(false);
        }
        return channelFailedToConnectAsync(0, getChannel().getState(true));
    }

    void shutdown() {
        if (channel != null) {
            channel.shutdown();
        }
    }

    void awaitTermination(long timeoutSeconds) throws InterruptedException {
        if (channel != null) {
            channel.awaitTermination(timeoutSeconds, TimeUnit.SECONDS);
        }
    }

    /**
     * Close the current nodes channel
     *
     * @param timeout
     * @throws InterruptedException
     */
    synchronized void close(Duration timeout) throws InterruptedException {
        if (channel != null) {
            channel.shutdown();
            channel.awaitTermination(timeout.getSeconds(), TimeUnit.SECONDS);
            channel = null;
        }
    }

    private String getUserAgent() {
        var thePackage = getClass().getPackage();
        var implementationVersion = thePackage != null ? thePackage.getImplementationVersion() : null;
        return "hedera-sdk-java/" + ((implementationVersion != null) ? ("v" + implementationVersion) : "DEV");
    }
}

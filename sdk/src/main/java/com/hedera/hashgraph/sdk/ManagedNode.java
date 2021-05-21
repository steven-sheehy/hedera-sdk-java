package com.hedera.hashgraph.sdk;

import io.grpc.LoadBalancerRegistry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

abstract class ManagedNode {
    String address;
    ManagedChannel channel;
    final ExecutorService executor;
    long lastUsed = 0;
    long useCount = 0;

    ManagedNode(String address, ExecutorService executor) {
        this.executor = executor;
        this.address = address;
    }

    synchronized void inUse() {
        useCount++;
        lastUsed = System.currentTimeMillis();
    }

    synchronized ManagedChannel getChannel() {
        if (channel != null) {
            return channel;
        }

        var registry = LoadBalancerRegistry.getDefaultRegistry();
        var protocols = new String[] {"TLSv1.2", "TLSv1.3"};
        var ciphers = Collections.singletonList("TLS_DHE_RSA_WITH_AES_256_GCM_SHA384");

        try {
            var NettyChannelBuilder = Class.forName("NettyChannelBuilder");
            var SslContextBuilder = Class.forName("SslContextBuilder");
            var GrpcSslContexts = Class.forName("GrpcSslContexts");

            var NettyChannelBuilder_forAddress = NettyChannelBuilder.getMethod("forAddress");
            NettyChannelBuilder_forAddress.invoke(address);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        SslContextBuilder contextBuilder = GrpcSslContexts.configure(SslContextBuilder.forClient());
        contextBuilder
            .protocols(protocols)
            .ciphers(ciphers, SupportedCipherSuiteFilter.INSTANCE);

        channel = NettyChannelBuilder
            .forAddress("35.237.182.66", 50212)
            .negotiationType(NegotiationType.TLS)
            .sslContext(contextBuilder.build())
            .overrideAuthority("127.0.0.1")
            .build();

        channel = ManagedChannelBuilder.forTarget(address)
            .usePlaintext()
            .userAgent(getUserAgent())
            .executor(executor)
            .build();

        return channel;
    }

    synchronized void close(long seconds) throws InterruptedException {
        if (channel != null) {
            channel.shutdown();
            channel.awaitTermination(seconds, TimeUnit.SECONDS);
            channel = null;
        }
    }

    private String getUserAgent() {
        var thePackage = getClass().getPackage();
        var implementationVersion = thePackage != null ? thePackage.getImplementationVersion() : null;
        return "hedera-sdk-java/" + ((implementationVersion != null) ? ("v" + implementationVersion) : "DEV");
    }
}

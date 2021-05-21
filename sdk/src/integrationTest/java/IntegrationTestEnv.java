import com.hedera.hashgraph.sdk.*;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NegotiationType;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SupportedCipherSuiteFilter;

import java.util.*;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class IntegrationTestEnv {
    public Client client;
    public PrivateKey operatorKey;
    public AccountId operatorId;
    public List<AccountId> nodeAccountIds;

    public static Random random = new Random();

    public IntegrationTestEnv() throws PrecheckStatusException, TimeoutException, ReceiptStatusException {
        if (System.getProperty("HEDERA_NETWORK").equals("previewnet")) {
            client = Client.forPreviewnet();
        } else {
            try {
                client = Client.fromConfigFile(System.getProperty("CONFIG_FILE"));
            } catch (Exception e) {
                client = Client.forTestnet();
            }
        }

        try {
            operatorKey = PrivateKey.fromString(System.getProperty("OPERATOR_KEY"));
            operatorId = AccountId.fromString(System.getProperty("OPERATOR_ID"));

            client.setOperator(operatorId, operatorKey);
        } catch (Exception e) {
        }

        assertNotNull(client.getOperatorAccountId());
        assertNotNull(client.getOperatorPublicKey());

        var key = PrivateKey.generate();

        var response = new AccountCreateTransaction()
            .setInitialBalance(new Hbar(130))
            .setKey(key)
            .execute(client);

        operatorId = Objects.requireNonNull(response.getReceipt(client).accountId);
        operatorKey = key;
        nodeAccountIds = Collections.singletonList(response.nodeId);
        client.setOperator(operatorId, operatorKey);

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
    }
}

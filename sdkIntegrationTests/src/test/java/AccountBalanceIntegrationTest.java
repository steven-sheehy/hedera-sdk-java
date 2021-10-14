package com.hedera.hashgraph.sdkIntegrationTests;

import com.hedera.hashgraph.sdk.AccountBalanceQuery;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.Hbar;
import com.hedera.hashgraph.sdk.PrecheckStatusException;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TokenCreateTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountBalanceIntegrationTest {
    @Test
    @DisplayName("can connect to previewnwet with TLS")
    void canConnectToPreviewnetWithTLS() throws Exception {
        HashMap<String, AccountId> network = new HashMap();
        network.put("0.previewnet.hedera.com:50212", new AccountId(3));
        network.put("1.previewnet.hedera.com:50212", new AccountId(4));
        network.put("2.previewnet.hedera.com:50212", new AccountId(5));
        network.put("3.previewnet.hedera.com:50212", new AccountId(6));
        network.put("4.previewnet.hedera.com:50212", new AccountId(7));

        Client client = Client.forNetwork(network);

        for (Map.Entry<String, AccountId> entry : network.entrySet()) {
            new AccountBalanceQuery()
                .setNodeAccountIds(Collections.singletonList(entry.getValue()))
                .setAccountId(entry.getValue())
                .execute(client);
        }

        client.close();
    }
}

package io.slingr.endpoints.ethereum;

import io.slingr.endpoints.services.DataStores;
import io.slingr.endpoints.services.datastores.DataStore;
import io.slingr.endpoints.utils.Json;
import io.slingr.endpoints.utils.tests.EndpointTests;
import io.slingr.endpoints.utils.tests.EndpointsServicesMock;
import io.slingr.endpoints.ws.exchange.FunctionRequest;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by dgaviola on 31/07/18.
 */
public class EthereumEndpointTest {
    private static EndpointTests test;
    private static EthereumEndpoint endpoint;

    @BeforeClass
    public static void init() throws Exception {
        /*test = EndpointTests.start(new io.slingr.endpoints.ethereum.Runner(), "test.properties");
        endpoint = (EthereumEndpoint) test.getEndpoint();*/
        endpoint = new EthereumEndpoint();
        List<String> validDataStores = new ArrayList<>();
        validDataStores.add("accounts");
        DataStores dataStores = new DataStores(new EndpointsServicesMock(), validDataStores, true);
        endpoint.accountsDs = new DataStore("accounts", dataStores);
    }

    @Test
    public void testAccountsManagement() {
        Json res = endpoint.createAccount(new FunctionRequest(Json.map()));
        assertNotNull(res.string("address"));
        String address = res.string("address");
        System.out.println("address: "+address);

        FunctionRequest request = new FunctionRequest(Json.map().set("params", Json.map().set("address", address)));
        Json account = endpoint.exportAccount(request);
        assertNotNull(account);
        assertEquals(account.string("address"), address);

        String privateKey = "f0786b43f1305389c7a60dabaf6b57ac16b1a254f8e7026513be4f82ec90d946";
        request = new FunctionRequest(Json.map().set("params", Json.map().set("privateKey", privateKey)));
        Json importedAccount = endpoint.importAccount(request);
        assertNotNull(importedAccount);
        assertEquals(importedAccount.string("address"), "0x0051bbFbaE99d2E8a90501eBFaD812FE39711283".toLowerCase());

        request = new FunctionRequest(Json.map().set("params", Json.map().set("address", "0x0051bbFbaE99d2E8a90501eBFaD812FE39711283".toLowerCase())));
        account = endpoint.exportAccount(request);
        assertNotNull(account);
        assertEquals(account.string("address"), "0x0051bbFbaE99d2E8a90501eBFaD812FE39711283".toLowerCase());
    }
}

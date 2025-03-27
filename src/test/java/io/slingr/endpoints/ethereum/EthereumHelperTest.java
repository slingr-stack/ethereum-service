package io.slingr.endpoints.ethereum;

import io.slingr.endpoints.utils.Json;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class EthereumHelperTest {

    private EthereumHelper ethereumHelper;
    private final String ABI_DEF_FILE = "abiDef.json";
    private final String ABI_TOKEN_FILE = "abiDefToken.json";
    private Json abiDef, abiDefToken;

    @Before
    public void initAbi() throws IOException {
        ethereumHelper = new EthereumHelper();
        abiDef = Json.fromInternalFile(ABI_DEF_FILE);
        abiDefToken = Json.fromInternalFile(ABI_TOKEN_FILE);
    }

    @Test
    public void checkFunctionSignature() {

        Json fnDef = ethereumHelper.getFunctionDefinition(abiDef, "setString");
        String fnSign = ethereumHelper.formatSignature(fnDef);
        Assert.assertEquals("setString(string)", fnSign);

        fnDef = ethereumHelper.getFunctionDefinition(abiDef, "getString");
        fnSign = ethereumHelper.formatSignature(fnDef);
        Assert.assertEquals("getString()", fnSign);

        fnDef = ethereumHelper.getFunctionDefinition(abiDef, "setBool");
        fnSign = ethereumHelper.formatSignature(fnDef);
        Assert.assertEquals("setBool(bool)", fnSign);

        fnDef = ethereumHelper.getFunctionDefinition(abiDef, "getBool");
        fnSign = ethereumHelper.formatSignature(fnDef);
        Assert.assertEquals("getBool()", fnSign);

        fnDef = ethereumHelper.getFunctionDefinition(abiDef, "setIntArr");
        fnSign = ethereumHelper.formatSignature(fnDef);
        Assert.assertEquals("setIntArr(int256,int256,int256)", fnSign);


        fnDef = ethereumHelper.getFunctionDefinition(abiDef, "setArrayUint");
        fnSign = ethereumHelper.formatSignature(fnDef);
        Assert.assertEquals("setArrayUint(uint256[2])", fnSign);

    }


    @Test
    public void testEncodeStringParams() {

        Json fnDef = ethereumHelper.getFunctionDefinition(abiDef, "setString");

        Json params = Json.list().push("Hello Guys!");

        String encodeArguments = ethereumHelper.encodeArguments(fnDef, params);
        String expectArguments = "00000000000000000000000000000000000000000000000000000000000000200000000000000000000000" +
                "00000000000000000000000000000000000000000b48656c6c6f204775797321000000000000000000000000000000000000000000";
        Assert.assertEquals(expectArguments, encodeArguments);

        String encodeFunction = ethereumHelper.encodeFunction(fnDef, params);
        String expectFunction = "7fcaf666" + expectArguments;
        Assert.assertEquals(expectFunction, encodeFunction);

        fnDef = ethereumHelper.getFunctionDefinition(abiDef, "getString");
        encodeFunction = ethereumHelper.encodeFunction(fnDef, null);
        expectFunction = "89ea642f";
        Assert.assertEquals(expectFunction, encodeFunction);

    }


    @Test
    public void testEncodeBoolParams() {

        Json fnDef = ethereumHelper.getFunctionDefinition(abiDef, "setBool");

        Json params = Json.list().push(true);

        String encodeFunction = ethereumHelper.encodeFunction(fnDef, params);
        String expectFunction = "1e26fd330000000000000000000000000000000000000000000000000000000000000001";
        Assert.assertEquals(expectFunction, encodeFunction);

        fnDef = ethereumHelper.getFunctionDefinition(abiDef, "getBool");
        encodeFunction = ethereumHelper.encodeFunction(fnDef, null);
        expectFunction = "12a7b914";
        Assert.assertEquals(expectFunction, encodeFunction);

    }

    @Test
    public void testEncodeIntParams() {

        Json fnDef = ethereumHelper.getFunctionDefinition(abiDef, "setInt");

        Json params = Json.list().push(345);

        String encodeFunction = ethereumHelper.encodeFunction(fnDef, params);
        String expectFunction = "747586b80000000000000000000000000000000000000000000000000000000000000159";
        Assert.assertEquals(expectFunction, encodeFunction);

        fnDef = ethereumHelper.getFunctionDefinition(abiDef, "getInt");
        encodeFunction = ethereumHelper.encodeFunction(fnDef, null);
        expectFunction = "62738998";
        Assert.assertEquals(expectFunction, encodeFunction);

        fnDef = ethereumHelper.getFunctionDefinition(abiDef, "setUint8");

        params = Json.list().push(52);

        encodeFunction = ethereumHelper.encodeFunction(fnDef, params);
        expectFunction = "1774e6460000000000000000000000000000000000000000000000000000000000000034";
        Assert.assertEquals(expectFunction, encodeFunction);

        fnDef = ethereumHelper.getFunctionDefinition(abiDef, "getUint8");
        encodeFunction = ethereumHelper.encodeFunction(fnDef, null);
        expectFunction = "343a875d";
        Assert.assertEquals(expectFunction, encodeFunction);

    }

    @Test
    public void testEncodeAddressParams() {

        Json fnDef = ethereumHelper.getFunctionDefinition(abiDef, "setAddress");

        Json params = Json.list().push("0xca35b7d915458ef540ade6068dfe2f44e8fa733c");

        String encodeFunction = ethereumHelper.encodeFunction(fnDef, params);
        String expectFunction = "e30081a0000000000000000000000000ca35b7d915458ef540ade6068dfe2f44e8fa733c";
        Assert.assertEquals(expectFunction, encodeFunction);

        fnDef = ethereumHelper.getFunctionDefinition(abiDef, "getAddress");
        encodeFunction = ethereumHelper.encodeFunction(fnDef, null);
        expectFunction = "38cc4831";
        Assert.assertEquals(expectFunction, encodeFunction);

    }

    @Test
    public void testEncodeByteAndUint8Params() {

        Json fnDef = ethereumHelper.getFunctionDefinition(abiDef, "addProperty");
        Json params = Json.list().push("0x32").push(3);
        String encodeFunction = ethereumHelper.encodeFunction(fnDef, params);
        String expectFunction = "b6d459a532000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000003";
        Assert.assertEquals(expectFunction, encodeFunction);
    }

    @Test
    public void testEncodeBytesParams() {

        Json fnDef = ethereumHelper.getFunctionDefinition(abiDef, "setBytes");

        String myBytes = "0xba51a6df0000000000000000000000000000000000000000000000000000000000000001";

        Json params = Json.list().push(myBytes);

        String encodeFunction = ethereumHelper.encodeFunction(fnDef, params);
        String expectFunction = "da359dc800000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000024ba51a6df000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000";
        Assert.assertEquals(expectFunction, encodeFunction);

        fnDef = ethereumHelper.getFunctionDefinition(abiDef, "getBytes");
        encodeFunction = ethereumHelper.encodeFunction(fnDef, null);
        expectFunction = "0bcd3b33";
        Assert.assertEquals(expectFunction, encodeFunction);

    }

    @Test
    public void testUintArrayParams() {

        Json fnDef = ethereumHelper.getFunctionDefinition(abiDef, "setArrayUint");

        Json uint = Json.list().push(34).push(12);
        Json params = Json.list().push(uint);

        String encodeFunction = ethereumHelper.encodeFunction(fnDef, params);
        String expectFunction = "1da366520000000000000000000000000000000000000000000000000000000000000022000000000000000" +
                "000000000000000000000000000000000000000000000000c";
        Assert.assertEquals(expectFunction, encodeFunction);

        fnDef = ethereumHelper.getFunctionDefinition(abiDef, "getUintArr");
        encodeFunction = ethereumHelper.encodeFunction(fnDef, null);
        expectFunction = "8a51f079";
        Assert.assertEquals(expectFunction, encodeFunction);

    }


    @Test
    public void testIntArrayParams() {

        Json fnDef = ethereumHelper.getFunctionDefinition(abiDef, "setIntArr");

        Json params = Json.list().push(2).push(3).push(4);

        String encodeFunction = ethereumHelper.encodeFunction(fnDef, params);
        String expectFunction = "8ac0d7570000000000000000000000000000000000000000000000000000000000000002000000000000000" +
                "0000000000000000000000000000000000000000000000003000000000000000000000000000000000000000000000000000000" +
                "0000000004";
        Assert.assertEquals(expectFunction, encodeFunction);

        fnDef = ethereumHelper.getFunctionDefinition(abiDef, "getIntArr");
        encodeFunction = ethereumHelper.encodeFunction(fnDef, null);
        expectFunction = "2453ccd3";
        Assert.assertEquals(expectFunction, encodeFunction);

    }

    @Test
    public void testBytes32() {

        Json fnDef = ethereumHelper.getFunctionDefinition(abiDef, "setBytes32");

        Json params = Json.list().push("0x0000000000000000000000000000000000000000000000000000000000000000");

        String encodeFunction = ethereumHelper.encodeFunction(fnDef, params);
        String expectFunction = "c2b12a730000000000000000000000000000000000000000000000000000000000000000";
        Assert.assertEquals(expectFunction, encodeFunction);
    }

    @Test
    public void testManyArrayParams() {

        Json fnDef = ethereumHelper.getFunctionDefinition(abiDef, "reserveTokens");

        Json params = Json.list();
        Json tranches = Json.list().push("0x0000000000000000000000000000000000000000000000000000000000000000")
                .push("0x0000000000000000000000000000000000000000000000000000000000000000")
                .push("0x0000000000000000000000000000000000000000000000000000000000000000");
        Json investors = Json.list().push("0x86B54084164Ebd9F91414Ab0c16Ff52cFa544Fde")
                .push("0x6736d0a08d7100B356671603C65CC27879bDE3eE")
                .push("0x54d0494803Dd47aEE19826f5BE7C0259Ee7f0Ae7");
        Json amounts = Json.list().push("0x64").push("0xc8").push("0x12c");
        params.push(tranches).push(investors).push(amounts);

        String encodeFunction = ethereumHelper.encodeFunction(fnDef, params);


        String expectFunction = "6c0db5d00000000000000000000000000000000000000000000000000000000000000060000000000" +
                "00000000000000000000000000000000000000000000000000000e000000000000000000000000000000000000000000000" +
                "000000000000000001600000000000000000000000000000000000000000000000000000000000000003000000000000000" +
                "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000" +
                "000000000000000000000000000000000000000000300000000000000000000000086b54084164ebd9f91414ab0c16ff52c" +
                "fa544fde0000000000000000000000006736d0a08d7100b356671603c65cc27879bde3ee00000000000000000000000054d" +
                "0494803dd47aee19826f5be7c0259ee7f0ae700000000000000000000000000000000000000000000000000000000000000" +
                "030000000000000000000000000000000000000000000000000000000000000064000000000000000000000000000000000" +
                "00000000000000000000000000000c8000000000000000000000000000000000000000000000000000000000000012c";
        Assert.assertEquals(expectFunction, encodeFunction);
    }

    @Test(expected = RuntimeException.class)
    public void checkInvalidParameters() {
        Json fnDef = ethereumHelper.getFunctionDefinition(abiDef, "setArrayUint");

        Json params = Json.list().push(2).push(3).push(4);

        ethereumHelper.encodeFunction(fnDef, params);

    }

    @Test
    public void decodeStringFunction() {

        Json fnDef = ethereumHelper.getFunctionDefinition(abiDef, "getString");
        String data = "0000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000" +
                "000000000000000000000000000000a4a75616e20506572657a00000000000000000000000000000000000000000000";

        Json result = ethereumHelper.decodeResult(fnDef, data);

        Assert.assertNotNull(result);
        Assert.assertEquals("Juan Perez", (String) result.object(0));

    }

    @Test
    public void decodeMultipleValues() {

        Json fnDef = ethereumHelper.getFunctionDefinition(abiDef, "result1");
        String data = "0x0000000000000000000000000000000000000000000000000000000000000060000000000000000000000000ca35b7d91" +
                "5458ef540ade6068dfe2f44e8fa733c000000000000000000000000000000000000000000000000000000000000023400000000" +
                "0000000000000000000000000000000000000000000000000000000a4a75616e20506572657a000000000000000000000000000" +
                "00000000000000000";

        Json result = ethereumHelper.decodeResult(fnDef, data);

        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("Juan Perez", (String) result.object(0));
        Assert.assertEquals("0xca35b7d915458ef540ade6068dfe2f44e8fa733c", result.object(1));
        Assert.assertEquals("0x234", result.object(2));

    }

    @Test
    public void decodeMultipleValuesWithArray() {

        Json fnDef = ethereumHelper.getFunctionDefinition(abiDef, "result2");
        String data = "0000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000" +
                "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000a000000000" +
                "0000000000000000000000000000000000000000000000000000000a4a75616e20506572657a000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000000000000000000030000000000000000000000" +
                "00000000000000000000000000000000000000000b0000000000000000000000000000000000000000000000000000000000000" +
                "0160000000000000000000000000000000000000000000000000000000000000021";

        Json result = ethereumHelper.decodeResult(fnDef, data);

        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("Juan Perez", (String) result.object(0));
        Assert.assertEquals(false, result.object(1));
        Object[] arr = {"0xb", "0x16", "0x21"};
        for (int i = 0; i < arr.length; i++) {
            Assert.assertEquals(arr[i], ((Object[]) result.object(2))[i]);
        }
    }

    @Test
    public void decodeMultipleValuesWithArrayAddresses() {

        Json fnDef = ethereumHelper.getFunctionDefinition(abiDef, "result3");
        String data = "0000000000000000000000000000000000000000000000000000000000000060000000000000000000000000000000000" +
                "000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000a000000000" +
                "0000000000000000000000000000000000000000000000000000000a4a75616e20506572657a000000000000000000000000000" +
                "0000000000000000000000000000000000000000000000000000000000000000000000000000000030000000000000000000000" +
                "00ca35b7d915458ef540ade6068dfe2f44e8fa733c000000000000000000000000ca35b7d915458ef540ade6068dfe2f44e8fa7" +
                "311000000000000000000000000ca35b7d915458ef540ade6068dfe2f44e8fa7333";

        Json result = ethereumHelper.decodeResult(fnDef, data);

        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("Juan Perez", (String) result.object(0));
        Assert.assertEquals(true, result.object(1));
        Object[] arr = {"0xca35b7d915458ef540ade6068dfe2f44e8fa733c", "0xca35b7d915458ef540ade6068dfe2f44e8fa7311",
                "0xca35b7d915458ef540ade6068dfe2f44e8fa7333"};

        for (int i = 0; i < arr.length; i++) {
            Assert.assertEquals(arr[i], ((Object[]) result.object(2))[i]);
        }

    }

    @Test
    public void decodeBytesType() {

        Json fnDef = ethereumHelper.getFunctionDefinition(abiDef, "getBytes");
        String data = "0x00000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000" +
                "000000000000000000000000000000024ba51a6df00000000000000000000000000000000000000000000000000000000000000" +
                "0200000000000000000000000000000000000000000000000000000000";

        Json result = ethereumHelper.decodeResult(fnDef, data);

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("0xba51a6df0000000000000000000000000000000000000000000000000000000000000002", result.toList().get(0));
    }

    @Test
    public void decodeBytes32Array() {
        Json fnDef = ethereumHelper.getFunctionDefinition(abiDef, "getBytes32Array");
        String data = "0x0000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000" +
                "00000000000000000000000000000010000000000000000000000000000000000000000000000000000000000000000";
        Json result = ethereumHelper.decodeResult(fnDef, data);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Object[] bytes32Array = (Object[]) result.toList().get(0);
        Assert.assertEquals(1, bytes32Array.length);
        Assert.assertEquals("0x0000000000000000000000000000000000000000000000000000000000000000",bytes32Array[0]);
    }

    @Test
    public void decodeBytes1() {
        Json fnDef = ethereumHelper.getFunctionDefinition(abiDef, "getByte");
        String data = "0x3200000000000000000000000000000000000000000000000000000000000000";
        Json result = ethereumHelper.decodeResult(fnDef, data);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("0x32", result.toList().get(0));
    }

    @Test
    public void decodeUint256() {
        Json fnDef = ethereumHelper.getFunctionDefinition(abiDef, "getUint256");
        String data = "0x00000000000000000000000000000000000000000000005150ae84a8cdf00000";
        Json result = ethereumHelper.decodeResult(fnDef, data);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("0x5150ae84a8cdf00000", result.toList().get(0));
    }

    @Test
    public void parseEvent() {

        Json evDef = ethereumHelper.getEventDefinition(abiDef, "MyEvent");
        String encodedEvent = ethereumHelper.encodeEvent(evDef);
        String expected = "417b479eab59fa49bedfbbb7cab907b3a81d8375cfca57da274a3d8b6c61a813";

        Assert.assertEquals(expected, encodedEvent);

    }

    @Test
    public void decodeEventResultWithIndexedFields() {

        Json evDef = ethereumHelper.getEventDefinition(abiDef, "NameChanged");
        String data = "0x0000000000000000000000000000000000000000000000000000000000000020000000000000000" +
                "00000000000000000000000000000000000000000000000057465737431000000000000000000000000000000000000000000000000000000";
        List<Object> topicsList = new ArrayList<>();
        topicsList.add("0x3b0a43ccc1ccd1c76ebb1a8d998fdfe1ded3766582dbbbcdda83889170bec53d");
        topicsList.add("0x000000000000000000000000590782dc744cb95662192bde0da32acf5e99d851");
        Json result = ethereumHelper.processResult(evDef, topicsList, data);

        Assert.assertNotNull(result);
        Json eventData = result.json("eventData");
        Assert.assertEquals("0x590782dc744cb95662192bde0da32acf5e99d851", eventData.string("user"));
        Assert.assertEquals("test1", eventData.string("newName"));

    }

    @Test
    public void decodeEventResult() {

        Json evDef = ethereumHelper.getEventDefinition(abiDef, "MyEvent");
        String data = "0x00000000000000000000000000000000000000000000000000000000000000400000000000000000000000000000000" +
                "000000000000000000000000000000065000000000000000000000000000000000000000000000000000000000000000a4a6f68" +
                "6e20477265656e00000000000000000000000000000000000000000000";
        Json result = ethereumHelper.decodeResult(evDef, data);

        Assert.assertNotNull(result);
        Assert.assertEquals("John Green", (String) result.object(0));
        Assert.assertEquals("0x65", result.object(1));

    }


    @Test
    public void testTokens(){

        String response = "{\n" +
                "            \"address\": \"0x9c0262aed31af73f9cb9b976890bb73a369332e6\",\n" +
                "            \"topics\": [\n" +
                "                \"0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef\",\n" +
                "                \"0x0000000000000000000000008bbf499511a6866d7e942afb1c2a12f9c065310b\",\n" +
                "                \"0x00000000000000000000000019d7b3090febf5596a9a0cf28c7172c53eaa351a\"\n" +
                "            ],\n" +
                "            \"data\": \"0x0000000000000000000000000000000000000000000000000000000005f5e100\",\n" +
                "            \"blockNumber\": \"0x303db5\",\n" +
                "            \"transactionHash\": \"0x82d367784cd304314455c297a1060c6c4ad7b272d13fe7fc8ce700b99d0d4d65\",\n" +
                "            \"transactionIndex\": \"0x2e\",\n" +
                "            \"blockHash\": \"0xa4b4a7ed55e449cc0ef33e43b3bfe3557be251816b84b65339857bc5ae22c189\",\n" +
                "            \"logIndex\": \"0xb\",\n" +
                "            \"removed\": false\n" +
                "        }";

        Json event = Json.parse(response);
        Json result = ethereumHelper.processResult(abiDefToken, event.json("topics").toList(), event.string("data"));

        Assert.assertNotNull(result);
        Assert.assertEquals("Transfer", result.string("eventName"));
        Assert.assertEquals(3, result.json("eventData").size());
        Assert.assertEquals("0x5f5e100", result.json("eventData").string("value"));
        Assert.assertEquals("0x8bbf499511a6866d7e942afb1c2a12f9c065310b", result.json("eventData").string("from"));
        Assert.assertEquals("0x19d7b3090febf5596a9a0cf28c7172c53eaa351a", result.json("eventData").string("to"));

    }

}

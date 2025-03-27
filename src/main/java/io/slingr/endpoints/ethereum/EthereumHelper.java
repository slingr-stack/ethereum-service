package io.slingr.endpoints.ethereum;

import io.slingr.endpoints.utils.Json;
import org.apache.commons.lang.StringUtils;
import org.ethereum.core.CallTransaction.Function;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;


public class EthereumHelper {


    public Json getFunctionDefinition(Json abiDef, String fnName) {
        return this.getDefinition(abiDef, fnName, "function");
    }

    public Json getEventDefinition(Json abiDef, String evName) {
        return this.getDefinition(abiDef, evName, "event");
    }

    public Json getDefinition(Json abiDef, String name, String type) {
        if (name == null) {
            return null;
        }
        for (Json def : abiDef.jsons()) {
            if (type.equals(def.string("type")) && name.equals(def.string("name"))) {
                return def;
            }
        }

        return null;
    }


    private Function getFunction(Json fnDef) {
        return Function.fromJsonInterface(fnDef.toString());
    }


    public String formatSignature(Json fnDef) {
        return getFunction(fnDef).formatSignature();
    }

    private Object[] getArguments(Json params) {
        List<Object> args = new ArrayList<>();
        if (params != null) {
            for (Object obj : params.toList()) {
                args.add(obj);
            }
        }
        return args.toArray();
    }

    public String encodeArguments(Json fnDef, Json params) {

        Function function = getFunction(fnDef);
        byte[] arguments = function.encodeArguments(getArguments(params));

        return Hex.toHexString(arguments);
    }

    private Json checkParams(Json fnDef, Json params) {

        if (params != null) {
            Json newParams = Json.list();

            List<Json> inputs = fnDef.jsons("inputs");
            List<Object> p = params.toList();

            if (inputs != null && p != null && inputs.size() == p.size()) {
                for (int i = 0; i < inputs.size(); i++) {

                    Object o = p.get(i);
                    String type = inputs.get(i).string("type");

                    if (type != null && type.startsWith("bytes")) {
                        if (type.endsWith("[]") && o instanceof List) {
                            List newList = new ArrayList();
                            for (Object item : ((List) o)) {
                                if (item instanceof String) {
                                    String value = ((String) item).startsWith("0x") ? ((String) item).substring(2) : (String) item;
                                    newList.add(Hex.decode(value.getBytes()));
                                } else {
                                    newList.add(item);
                                }
                            }
                            newParams.push(newList);
                        } else {
                            if (o instanceof String) {
                                // now we need to check if we need to add right padding for shortcuts like bytes1, bytes2, etc.
                                String bytesQty = type.substring("bytes".length());
                                String value = ((String) o).startsWith("0x") ? ((String) o).substring(2) : (String) o;
                                if (!StringUtils.isBlank(bytesQty) && StringUtils.isNumeric(bytesQty)) {
                                    int bytesSize = Integer.parseInt(bytesQty);
                                    value = StringUtils.leftPad(value, bytesSize * 2, '0');
                                    value = StringUtils.rightPad(value, 64, '0');
                                }
                                newParams.push(Hex.decode(value.getBytes()));
                            } else {
                                newParams.push(o);
                            }
                        }
                    } else {
                        newParams.push(o);
                    }
                }

                return newParams;

            }


        }

        return params;
    }


    public String encodeFunction(Json fnDef, Json params) {

        params = checkParams(fnDef, params);

        Function function = getFunction(fnDef);
        byte[] arguments = function.encode(getArguments(params));

        return Hex.toHexString(arguments);
    }

    public String encodeEvent(Json evDef) {
        Function function = getFunction(evDef);
        byte[] arguments = function.encodeSignatureLong();

        return Hex.toHexString(arguments);
    }

    public String removeHexStringToData(String data) {
        if (data != null && data.startsWith("0x")) {
            return data.substring(2);
        }
        return data;
    }

    public Json decodeResult(Json fnDef, String data) {

        data = removeHexStringToData(data);

        Function function = getFunction(fnDef);

        if ("event".equals(fnDef.string("type"))) {
            function.outputs = function.inputs;
            fnDef.set("outputs", fnDef.json("inputs"));
        }

        Object[] res = function.decodeResult(Hex.decode(data.getBytes()));

        if (res != null && res.length > 0) {

            for (int i = 0; i < res.length; i++) {
                Json dt = (Json) fnDef.json("outputs").objects().get(i);
                String type = dt.string("type");
                if (isBytes(type) && type.endsWith("[]")) {
                    Object[] add = (Object[]) res[i];
                    for (int j = 0; j < add.length; j++) {
                        if (add[j] instanceof byte[]) {
                            add[j] = "0x" + Hex.toHexString((byte[]) add[j]);
                        }
                    }
                } else if (isNumber(type) && type.endsWith("[]")) {
                        Object[] add = (Object[]) res[i];
                        for (int j = 0; j < add.length; j++) {
                            if (add[j] instanceof BigInteger) {
                                add[j] = "0x" + ((BigInteger) add[j]).toString(16);
                            }
                        }
                } else if (isBytes(type) && res[i] instanceof byte[]) {
                    String bytesQty = type.substring("bytes".length());
                    String value = Hex.toHexString((byte[]) res[i]);
                    if (!StringUtils.isBlank(bytesQty) && StringUtils.isNumeric(bytesQty)) {
                        int bytesSize = Integer.parseInt(bytesQty);
                        value = value.substring(0, bytesSize * 2);
                    }
                    res[i] = "0x" + value;
                } else if (isNumber(type) && res[i] instanceof BigInteger) {
                    res[i] = "0x" + ((BigInteger) res[i]).toString(16);
                }
            }

        }

        Json resObj = Json.list();
        for (Object o : res) {
            resObj.push(o);
        }

        return resObj;
    }

    private boolean isBytes(String type) {
        if (type != null && (type.startsWith("address") || type.startsWith("bytes"))) {
            return true;
        }

        return false;
    }

    private boolean isNumber(String type) {
        if (type != null && (type.startsWith("int") || type.startsWith("uint"))) {
            return true;
        }

        return false;
    }

    public Json splitAbi(Json abi, boolean isIndexes) {

        Json filter = Json.list();

        for (Json a : abi.json("inputs").jsons()) {
            if (a.bool("indexed") && isIndexes) {
                filter.push(a);
            } else if (!a.bool("indexed") && !isIndexes) {
                filter.push(a);
            }
        }

        return filter;
    }

    public Object getKeyValue(Json inputs, Json values, int index, List<Json> indexes, List<Object> topics) {
        Json r = Json.map();

        for (Json input : inputs.jsons()) {
            r.set(input.string("name"), values.object(index));
            index++;
        }

        if (indexes.size() == topics.size() - 1) {
            for (int i = 0; i < indexes.size(); i++) {
                Object data = this.decodeDataType(indexes.get(i), topics.get(i + 1));
                r.set((indexes.get(i)).string("name"), data);
            }
        }

        return r;
    }

    private Object decodeDataType(Json dt, Object data) {

        Json result = this.decodeResult(Json.map().set("outputs", Json.list().push(dt)), (String) data);

        if (result.size() > 0) {
            return result.toList().get(0);
        }

        return null;

    }

    public Json processResult(Json abi, List<Object> topicsList, String data) {
        Json result = Json.map();

        Json abiCloned = Json.parse(abi.toString());
        Json indexes = this.splitAbi(abiCloned, true);
        abiCloned.set("inputs", this.splitAbi(abiCloned, false));

        Json values = this.decodeResult(abiCloned, data);

        result.set("eventName", abi.string("name"));
        result.set("eventData", this.getKeyValue(abiCloned.json("inputs"), values, 0, indexes.jsons(), topicsList));
        return result;
    }

    public static long convertedHexToNumber(String number) {
        return Long.parseLong(number.startsWith("0x") ? number.substring(2) : number, 16);
    }

    public static String convertNumberToHex(long number) {
        return "0x" + Long.toHexString(number);
    }

}

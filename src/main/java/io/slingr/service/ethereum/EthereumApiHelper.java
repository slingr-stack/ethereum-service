package io.slingr.service.ethereum;

import io.slingr.services.HttpService;
import io.slingr.services.utils.Json;
import io.slingr.services.ws.exchange.FunctionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EthereumApiHelper {
    private static final Logger logger = LoggerFactory.getLogger(EthereumApiHelper.class);

    private final HttpService httpService;

    public EthereumApiHelper(HttpService httpService) {
        this.httpService = httpService;
    }

    public Json getBlockByHash(String blockHash, boolean fullTransactions) {
        Json body = this.getBody("eth_getBlockByHash", Json.list().push(blockHash).push(fullTransactions));
        logger.debug("Get Block by Hash: {} fullTransactions: {}", blockHash, fullTransactions);
        logger.debug("Body: {}", body.toString());
        Json response = postAndGetResponse(body);
        return response != null ? response.json("result") : null;
    }

    public Json getBlockByNumber(String number, boolean fullTransactions) {
        Json body = this.getBody("eth_getBlockByNumber", Json.list().push(number).push(fullTransactions));
        logger.debug("Get Block by Number: {} fullTransactions: {}", number, fullTransactions);
        logger.debug("Body: {}", body.toString());
        Json response = postAndGetResponse(body);
        return response != null ? response.json("result") : null;
    }

    public Json getTransactionReceipt(String txHash) {
        Json body = this.getBody("eth_getTransactionReceipt", Json.list().push(txHash));
        logger.debug("Get transaction: {}", txHash);
        logger.debug("Body: {}", body.toString());
        Json response = postAndGetResponse(body);
        return response != null ? response.json("result") : null;
    }

    public List<Json> getLogsByBlock(String hash) {
        Json body = this.getBody("eth_getLogs", Json.list().push(Json.map().set("blockHash", hash)));
        logger.debug("Get logs by block: {}", hash);
        logger.debug("Body: {}", body.toString());
        Json response = postAndGetResponse(body);
        return response != null && response.jsons("result") != null ? response.jsons("result") : new ArrayList<>();
    }

    private Json getBody(String method, Json params) {
        Json requestParams = Json.map().set("url", httpService.getApiUri()).set("body", Json.map()
                .set("jsonrpc", "2.0")
                .set("method", method)
                .set("params", params)
                .set("id", 1));
        return Json.map().set("id", new Date().getTime()).set("params", requestParams);

    }

    protected Json postAndGetResponse(Json body) {
        try {
            return this.httpService.defaultPostRequest(new FunctionRequest(body));
        } catch (Exception e) {
            logger.error("Error posting json: {}", body.toString());
            logger.error(e.getMessage(), e);
        }
        return null;
    }
}

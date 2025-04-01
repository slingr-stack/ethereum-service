package io.slingr.service.ethereum;

import io.slingr.services.Service;
import io.slingr.services.HttpService;
import io.slingr.services.configurations.*;
import io.slingr.services.exceptions.ServiceException;
import io.slingr.services.exceptions.ErrorCode;
import io.slingr.services.framework.annotations.*;
import io.slingr.services.services.AppLogs;
import io.slingr.services.services.datastores.DataStore;
import io.slingr.services.services.datastores.DataStoreResponse;
import io.slingr.services.utils.Json;
import io.slingr.services.ws.exchange.FunctionRequest;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.ethereum.crypto.ECKey;
import org.ethereum.crypto.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.ethereum.util.ByteUtil.longToBytesNoLeadZeroes;

/**
 * <p>Ethereum endpoint
 * <p>
 * <p>Created by hpacini on 12/03/18.
 */
@SlingrService(name = "ethereum")
public class Ethereum extends HttpService {

    private static final Logger logger = LoggerFactory.getLogger(Ethereum.class);

    private final int ACCOUNTS_SEED_LENGTH = 128;
    private final String ENCRYPTION_PASSWORD = "jdU72Jus72bnfOnzA82su!8s_27hsN0jsy#";
    private final String NEW_BLOCK_EVENT = "newBlock";
    private final String BLOCK_REMOVED_EVENT = "blockRemoved";

    @ApplicationLogger
    private AppLogs appLogger;

    @ServiceConfiguration
    private Json configuration;

    @ServiceDataStore(name = "contracts")
    private DataStore contractsDs;

    @ServiceDataStore(name = "blocks")
    private DataStore blocksDs;

    @ServiceDataStore(name = "transactions")
    private DataStore transactionsDs;

    @ServiceDataStore(name = "events")
    private DataStore eventsDs;

    @ServiceDataStore(name = "accounts")
    public DataStore accountsDs;

    private String networkUrl;

    private EthereumHelper ethereumHelper;
    private EthereumApiHelper ethereumApiHelper;
    private SolidityUtils solidityUtils;
    private BlocksManager blocksManager;
    private TransactionManager transactionManager;
    private EventsManager eventsManager;
    private CryptoUtils cryptoUtils;

    private final static String CONFIRMATION_TIMEOUT_PROPERTY = "confirmationTimeout";
    private final static long DEFAULT_CONFIRATION_TIMEOUT = 1800;
    private final static String CONFIRMATION_BLOCKS_PROPERTY = "confirmationBlocks";
    private final static long DEFAULT_CONFIRMATION_BLOCKS = 0;
    public final static String MULTITENANCY_PROPERTY = "shared";
    public final static boolean DEFAULT_MULTITENANCY_PROPERTY = false;

    private long confirmationTimeout;
    private long confirmationBlocks;
    private boolean isShared;


    public String getApiUri() {
        switch (configuration.string("service")) {
            case "infura":
                networkUrl = "https://" + configuration.string("infuraNetwork") + ".infura.io/v3/" + configuration.string("infuraApiKey");
                break;

            case "custom":
                networkUrl = configuration.string("customNodeUrl");
                break;

            default:
                appLogger.error("Invalid service configuration");
                throw ServiceException.permanent(ErrorCode.ARGUMENT, "Invalid service configuration");
        }
        appLogger.info(String.format("Ethereum node URI: %s", networkUrl));
        return networkUrl;
    }

    public void endpointStarted() {
        ethereumHelper = new EthereumHelper();
        ethereumApiHelper = new EthereumApiHelper(httpService());
        solidityUtils = new SolidityUtils();
        try {
            cryptoUtils = new CryptoUtils(ENCRYPTION_PASSWORD);
        } catch (Exception e) {
            appLogger.error("Error initializing encryption utilities", e);
        }
        if (!properties().isLocalDeployment()) {
            try {
                solidityUtils.exportResource("solc");
            } catch (Exception ex) {
                logger.error("Error loading Solidity compiler", ex);
                appLogger.error("Error loading Solidity compiler", ex);
            }
        }

        this.confirmationTimeout = TimeUnit.SECONDS.toMillis(configuration.longInteger(CONFIRMATION_TIMEOUT_PROPERTY, DEFAULT_CONFIRATION_TIMEOUT));
        this.confirmationBlocks = configuration.longInteger(CONFIRMATION_BLOCKS_PROPERTY, DEFAULT_CONFIRMATION_BLOCKS);
        this.isShared = configuration.bool(MULTITENANCY_PROPERTY, DEFAULT_MULTITENANCY_PROPERTY);
        if (isShared) {
            logger.info("Endpoint started as a shared instance");
        }
        //this.httpService.setDefaultEmptyPath("");

        transactionManager = new TransactionManager(ethereumApiHelper, events(), appLogger, transactionsDs, configuration);
        transactionManager.start();
        eventsManager = new EventsManager(ethereumApiHelper, events(), appLogger, eventsDs, contractsDs, configuration.jsons("contracts"), confirmationBlocks, configuration);
        eventsManager.start();
        blocksManager = new BlocksManager(ethereumApiHelper, appLogger, blocksDs, new EthereumEvent() {
            @Override
            public void onNewBlock(Block block) {
                transactionManager.processTransactionsInBlock(block);
                eventsManager.processEventsInBlock(block);
                if (!isShared) {
                    events().send(NEW_BLOCK_EVENT, block.getOriginalBlockInfo());
                }
            }

            @Override
            public void onRemovedBlock(Block block) {
                transactionManager.removeTransactionsInBlock(block);
                eventsManager.removeEventsInBlock(block);
                if (!isShared) {
                    events().send(BLOCK_REMOVED_EVENT, block.getOriginalBlockInfo());
                }
            }
        }, configuration);
        blocksManager.start();
    }

    @ServiceFunction(name = "registerContract")
    public Json registerContract(FunctionRequest request) {
        Json body = request.getJsonParams();
        Json query = Json.map().set("address", body.string("address").toLowerCase());
        Json existingContract = contractsDs.findOne(query);
        if (existingContract != null) {
            throw new IllegalArgumentException("Another contract is already registered on this address");
        }
        body.set("address", body.string("address").toLowerCase());
        if (StringUtils.equalsIgnoreCase(body.string("address"), body.string("alias"))) {
            body.set("alias", body.string("alias").toLowerCase());
        }
        Json contract = contractsDs.save(body);
        eventsManager.registerContract(contract.string("address").toLowerCase(), contract.json("abi"));
        return contract;
    }

    @ServiceFunction(name = "getContract")
    public Json getContract(FunctionRequest request) {
        Json body = request.getJsonParams();

        String aliasOrAddress = body.string("aliasOrAddress");
        if (aliasOrAddress == null) {
            throw new IllegalArgumentException(String.format("Contract with alias or address [%s] was not found", aliasOrAddress));
        }
        boolean isAddress = body.bool("isAddress");
        if (isAddress) {
            aliasOrAddress = aliasOrAddress.toLowerCase();
        }

        // find in endpoint's config
        List<Json> contracts = configuration.jsons("contracts");
        Json contract = filterContract(isAddress, aliasOrAddress, contracts);
        if (contract != null) {
            return contract;
        }

        // find in dynamic contracts

        String key = isAddress ? "address" : "alias";
        contract = contractsDs.findOne(Json.map().set(key, aliasOrAddress));

        //TODO: should be removed when filter on data store implement 'OR'
        if (contract == null) {
            return contractsDs.findOne(Json.map().set(key, aliasOrAddress.toLowerCase()));
        }

        return contract;
    }

    private Json filterContract(boolean isAddress, String addressOrAlias, List<Json> contracts) {
        if (contracts != null) {
            for (Json co : contracts) {

                String alias = co.string("alias");
                String address = co.string("address");

                if (!isAddress && addressOrAlias.equals(alias) ||
                        isAddress && address != null && addressOrAlias.toLowerCase().equals(address.toLowerCase())) {
                    if (co.object("abi") instanceof String) {
                        co.set("abi", Json.parse(co.string("abi")));
                    }
                    return co;
                }
            }
        }
        return null;
    }

    @ServiceFunction(name = "removeContract")
    public Json removeContract(FunctionRequest request) {
        Json body = request.getJsonParams();
        String aliasOrAddress = body.string("alias");
        boolean isAddress = body.bool("isAddress");
        if (isAddress) {
            aliasOrAddress = aliasOrAddress.toLowerCase();
        }

        // find in endpoint's config
        List<Json> contracts = configuration.jsons("contracts");
        if (contracts != null) {
            for (Json co : contracts) {
                if (!isAddress && aliasOrAddress.equals(co.string("alias")) ||
                        isAddress && aliasOrAddress.equals(co.string("address").toLowerCase())) {
                    throw new IllegalArgumentException("This contract is configured in the endpoint and cannot be removed");
                }
            }
        }
        // find in dynamic contracts
        String key = isAddress ? "address" : "alias";
        Json co = contractsDs.findOne(Json.map().set(key, aliasOrAddress));

        //TODO: should be removed when filter on data store implement 'OR'
        if (co == null) {
            co = contractsDs.findOne(Json.map().set(key, aliasOrAddress));
        }
        if (co != null) {
            eventsManager.removeContract(co.string("address"));
            contractsDs.remove(Json.map().set(key, aliasOrAddress));
            return co;
        }
        throw new IllegalArgumentException(String.format("Contract with alias or address [%s] was not found", aliasOrAddress));
    }


    @ServiceFunction(name = "encodedFunction")
    public Json encodedFunction(FunctionRequest request) {
        Json body = request.getJsonParams();
        return Json.map().set("body", "0x" + ethereumHelper.encodeFunction(body.json("fnAbi"), body.json("params")));
    }

    @ServiceFunction(name = "decodeFunction")
    public Json decodedResult(FunctionRequest request) {
        Json body = request.getJsonParams();
        return ethereumHelper.decodeResult(body.json("fnAbi"), body.string("data"));
    }

    @ServiceFunction(name = "compileSolidity")
    public Json compileSolidity(FunctionRequest request) {
        Json body = request.getJsonParams();
        String sourceCode = body.string("code");
        Json libraries = body.json("libraries");
        return solidityUtils.compile(sourceCode, libraries);
    }

    @ServiceFunction(name = "confirmTransaction")
    public Json confirmTransaction(FunctionRequest request) {
        Json body = request.getJsonParams();
        long confirmationTimeout, confirmationBlocks;
        if (body.longInteger(CONFIRMATION_TIMEOUT_PROPERTY) != null) {
            confirmationTimeout = TimeUnit.SECONDS.toMillis(body.longInteger(CONFIRMATION_TIMEOUT_PROPERTY));
        } else {
            confirmationTimeout = this.confirmationTimeout;
        }
        if (body.integer(CONFIRMATION_BLOCKS_PROPERTY) != null) {
            confirmationBlocks = body.integer(CONFIRMATION_BLOCKS_PROPERTY);
        } else {
            confirmationBlocks = this.confirmationBlocks;
        }
        long timestamp = new Date().getTime();
        String txHash = body.string(Transaction.TX_HASH);
        confirmationTimeout = confirmationTimeout + timestamp;
        String nonce = body.string(Transaction.NONCE);
        String from = body.string(Transaction.FROM);
        transactionManager.registerTransaction(txHash, nonce, from, request.getFunctionId(), timestamp, confirmationTimeout, confirmationBlocks);
        Json resp = Json.map();
        resp.set("status", "ok");
        return resp;
    }

    @ServiceFunction(name = "createAccount")
    public Json createAccount(FunctionRequest request) {
        BigInteger privateKeyInteger;
        do {
            String seed = RandomStringUtils.random(ACCOUNTS_SEED_LENGTH);
            byte[] privateKeyBytes = HashUtil.sha3(seed.getBytes());
            privateKeyInteger = new BigInteger(privateKeyBytes);
        }
        while (privateKeyInteger.signum() < 0); // if it is negative there are problems, so iterate until we generate a positive number
        String privateKey = privateKeyInteger.toString(16);
        ECKey key = ECKey.fromPrivate(privateKeyInteger);
        String address = "0x" + Hex.toHexString(key.getAddress());
        Json account = Json.map()
                .set("address", address)
                .set("privateKey", cryptoUtils.encrypt(privateKey));
        accountsDs.save(account);
        return Json.map()
                .set("address", address);
    }

    @ServiceFunction(name = "importAccount")
    public Json importAccount(FunctionRequest request) {
        Json body = request.getJsonParams();
        String privateKey = body.string("privateKey");
        if (StringUtils.isBlank(privateKey)) {
            throw new IllegalArgumentException("Private key must be provided");
        }
        BigInteger privateKeyInteger = null;
        try {
            privateKey = ethereumHelper.removeHexStringToData(privateKey);
            privateKeyInteger = new BigInteger(privateKey, 16);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid private key. Must be an hexadecimal string.", e);
        }
        ECKey key = ECKey.fromPrivate(privateKeyInteger);
        String address = "0x" + Hex.toHexString(key.getAddress());
        Json existingAccount = accountsDs.findOne(Json.map().set("address", address));
        if (existingAccount != null) {
            throw new IllegalArgumentException("An account with that address already exists");
        }
        Json account = Json.map()
                .set("address", address)
                .set("privateKey", cryptoUtils.encrypt(privateKey));
        accountsDs.save(account);
        return Json.map()
                .set("address", address);
    }

    @ServiceFunction(name = "exportAccount")
    public Json exportAccount(FunctionRequest request) {
        Json body = request.getJsonParams();
        String address = body.string("address");
        if (StringUtils.isBlank(address)) {
            throw new IllegalArgumentException("Address must be provided");
        }
        if (!address.startsWith("0x")) {
            address = "0x" + address;
        }
        Json account = accountsDs.findOne(Json.map().set("address", address.toLowerCase()));
        if (account != null) {
            account.set("privateKey", cryptoUtils.decrypt(account.string("privateKey")));
        }
        return account;
    }

    @ServiceFunction(name = "signTransaction")
    public Json signTransaction(FunctionRequest request) {
        Json body = request.getJsonParams();
        String fromAddress = body.string("from");
        if (StringUtils.isBlank(fromAddress)) {
            throw new IllegalArgumentException("From address must be provided");
        }
        if (!fromAddress.startsWith("0x")) {
            fromAddress = "0x" + fromAddress;
        }
        Json account = accountsDs.findOne(Json.map().set("address", fromAddress.toLowerCase()));
        if (account == null) {
            throw new IllegalArgumentException(String.format("Address [%s] is not managed", fromAddress));
        }
        BigInteger privateKeyInteger = new BigInteger(cryptoUtils.decrypt(account.string("privateKey")), 16);
        ECKey key = ECKey.fromPrivate(privateKeyInteger);
        org.ethereum.core.Transaction tx = new org.ethereum.core.Transaction(
                body.isEmpty("nonce") ? null : longToBytesNoLeadZeroes(Long.parseUnsignedLong(ethereumHelper.removeHexStringToData(body.string("nonce")), 16)),
                body.isEmpty("gasPrice") ? null : longToBytesNoLeadZeroes(Long.parseUnsignedLong(ethereumHelper.removeHexStringToData(body.string("gasPrice")), 16)),
                body.isEmpty("gas") ? null : longToBytesNoLeadZeroes(Long.parseUnsignedLong(ethereumHelper.removeHexStringToData(body.string("gas")), 16)),
                body.isEmpty("to") ? null : Hex.decode(ethereumHelper.removeHexStringToData(body.string("to"))),
                body.isEmpty("value") ? longToBytesNoLeadZeroes(Long.valueOf(0)) : longToBytesNoLeadZeroes(Long.parseUnsignedLong(ethereumHelper.removeHexStringToData(body.string("value")), 16)),
                body.isEmpty("data") ? null : Hex.decode(ethereumHelper.removeHexStringToData(body.string("data"))),
                body.integer("netId")
        );
        tx.sign(key);
        return Json.map().set("data", "0x" + Hex.toHexString(tx.getEncoded()));
    }

    @ServiceFunction(name = "decodeLogsInReceipt")
    public Json decodeLogsInReceipt(FunctionRequest request) {
        Json body = request.getJsonParams();
        List<Json> logs = body.jsons("logs");
        Json events = Json.list();
        if (logs != null) {
            for (Json log : logs) {
                Json parsedLog = eventsManager.decodeEvent(log.string("address"), log.objects("topics"), log.string("data"));
                if (parsedLog != null) {
                    Json event = Json.map();
                    event.set("rawEvent", log);
                    event.set("eventName", parsedLog.string("eventName"));
                    event.set("eventData", parsedLog.json("eventData"));
                    events.push(event);
                }
            }
        }
        return events;
    }
}

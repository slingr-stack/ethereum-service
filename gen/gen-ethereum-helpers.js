var fs = require('fs');

var FILE_NAME = "ethereum-fn-helpers.js";
var CODE = '';

var jsonRPC = [

    {rpc: 'web3_clientVersion', namespace: 'web3', methodName: 'clientVersion', params: [], async: false},
    {rpc: 'web3_sha3', namespace: 'web3', methodName: 'sha3', params: ['data'], async: false},


    {rpc: 'net_version', namespace: 'net', methodName: 'version', params: [], async: false},
    {rpc: 'net_peerCount', namespace: 'net', methodName: 'peerCount', params: [], async: false},
    {rpc: 'net_listening', namespace: 'net', methodName: 'listening', params: [], async: false},

    {rpc: 'eth_protocolVersion', namespace: 'eth', methodName: 'protocolVersion', params: [], async: false},
    {rpc: 'eth_syncing', namespace: 'eth', methodName: 'syncing', params: [], async: false},
    {rpc: 'eth_coinbase', namespace: 'eth', methodName: 'coinbase', params: [], async: false, forbidden: true},
    {rpc: 'eth_coinbase', namespace: 'eth', methodName: 'getCoinbase', params: [], async: true, forbidden: true},
    {rpc: 'eth_mining', namespace: 'eth', methodName: 'mining', params: [], async: false, forbidden: true},
    {rpc: 'eth_hashrate', namespace: 'eth', methodName: 'hashrate', params: [], async: false, forbidden: true},
    {rpc: 'eth_gasPrice', namespace: 'eth', methodName: 'gasPrice', params: [], async: false},
    {rpc: 'eth_accounts', namespace: 'eth', methodName: 'accounts', params: [], async: false, forbidden: true},
    {rpc: 'eth_blockNumber', namespace: 'eth', methodName: 'blockNumber', params: [], async: false},
    {rpc: 'eth_getBalance', namespace: 'eth', methodName: 'getBalance', params: ['address', 'defaultBlock'], async: true},
    {rpc: 'eth_getStorageAt', namespace: 'eth', methodName: 'getStorageAt', params: ['address', 'position', 'defaultBlock'], async: true},
    {rpc: 'eth_getTransactionCount', namespace: 'eth', methodName: 'transactionCount', params: ['address', 'defaultBlock'], async: false},
    {rpc: 'eth_getBlockTransactionCountByHash', namespace: 'eth', methodName: 'getBlockTransactionCountByHash', params: ['data'], async: true},
    {rpc: 'eth_getBlockTransactionCountByNumber', namespace: 'eth', methodName: 'getBlockTransactionCountByNumber', params: ['quantityOrTag'], async: true},
    {rpc: 'eth_getUncleCountByBlockHash', namespace: 'eth', methodName: 'getUncleCountByBlockHash', params: ['data'], async: true},
    {rpc: 'eth_getUncleCountByBlockNumber', namespace: 'eth', methodName: 'getUncleCountByBlockNumber', params: ['quantityOrTag'], async: true},
    {rpc: 'eth_getCode', namespace: 'eth', methodName: 'getCode', params: ['address', 'defaultBlock'], async: true},
    {rpc: 'eth_sign', namespace: 'eth', methodName: 'sign', params: ['address', 'message'], async: false, forbidden: true},
    {rpc: 'eth_sendTransaction', namespace: 'eth', methodName: 'sendTransaction', params: ['ethObject'], async: true, forbidden: true},
    {rpc: 'eth_sendRawTransaction', namespace: 'eth', methodName: 'sendRawTransaction', params: ['data'], async: true},
    {rpc: 'eth_call', namespace: 'eth', methodName: 'call', params: ['callObject', 'defaultBlock'], async: true},
    {rpc: 'eth_estimateGas', namespace: 'eth', methodName: 'estimateGas', params: ['ethObject'], async: false},
    {rpc: 'eth_getBlockByHash', namespace: 'eth', methodName: 'blockByHash', params: ['data', 'isReturnedFullObject'], async: false},
    {rpc: 'eth_getBlockByNumber', namespace: 'eth', methodName: 'blockByNumber', params: ['data', 'isReturnedFullObject'], async: false},
    {rpc: 'eth_getTransactionByHash', namespace: 'eth', methodName: 'transactionByHash', params: ['data'], async: false},
    {rpc: 'eth_getTransactionByBlockHashAndIndex', namespace: 'eth', methodName: 'transactionByBlockHashAndIndex', params: ['data', 'quantity'], async: false},
    {rpc: 'eth_getTransactionByBlockNumberAndIndex', namespace: 'eth', methodName: 'transactionByBlockNumberAndIndex', params: ['blockNumber', 'quantity'], async: false},
    {rpc: 'eth_getTransactionReceipt', namespace: 'eth', methodName: 'transactionReceipt', params: ['data'], async: false},
    {rpc: 'eth_getUncleByBlockHashAndIndex', namespace: 'eth', methodName: 'uncleByBlockHashAndIndex', params: ['data', 'quantity'], async: false},
    {rpc: 'eth_getUncleByBlockNumberAndIndex', namespace: 'eth', methodName: 'uncleByBlockNumberAndIndex', params: ['quantityOrTag', 'quantity'], async: false},
    {rpc: 'eth_getCompilers', namespace: 'eth', methodName: 'eth_getCompilers', params: [], async: false, forbidden: true},
    {rpc: 'eth_compileSolidity', namespace: 'eth', methodName: 'compileSolidity', params: ['sourceString'], async: false, forbidden: true},
    {rpc: 'eth_compileLLL', namespace: 'eth', methodName: 'compileLLL', params: ['sourceString'], async: false, forbidden: true},
    {rpc: 'eth_compileSerpent', namespace: 'eth', methodName: 'compileSerpent', params: ['sourceString'], async: false, forbidden: true},
    {rpc: 'eth_newFilter', namespace: 'eth', methodName: 'newFilter', params: ['filterOptions'], async: false, forbidden: true},
    {rpc: 'eth_newBlockFilter', namespace: 'eth', methodName: 'newBlockFilter', params: [], async: false, forbidden: true},
    {rpc: 'eth_newPendingTransactionFilter', namespace: 'eth', methodName: 'newPendingTransactionFilter', params: [], async: false, forbidden: true},
    {rpc: 'eth_uninstallFilter', namespace: 'eth', methodName: 'uninstallFilter', params: ['quantity'], async: false, forbidden: true},
    {rpc: 'eth_getFilterChanges', namespace: 'eth', methodName: 'filterChanges', params: ['quantity'], async: false, forbidden: true},
    {rpc: 'eth_getFilterLogs', namespace: 'eth', methodName: 'filterLogs', params: ['quantity'], async: false, forbidden: true},
    {rpc: 'eth_getLogs', namespace: 'eth', methodName: 'getLogs', params: ['filterObject'], async: true, forbidden: true},
    {rpc: 'eth_getWork', namespace: 'eth', methodName: 'getWork', params: [], async: true},
    {rpc: 'eth_submitWork', namespace: 'eth', methodName: 'submitWork', params: ['nonce', 'headres', 'mixDigest'], async: true, forbidden: true},
    {rpc: 'eth_submitHashrate', namespace: 'eth', methodName: 'submitHashrate', params: ['hashrate', 'clientId'], async: true, forbidden: true},


    {rpc: 'db_putString', namespace: 'db', methodName: 'putString', params: ['db', 'key', 'value'], async: false, forbidden: true},
    {rpc: 'db_getString', namespace: 'db', methodName: 'getString', params: ['db', 'key'], async: false, forbidden: true},
    {rpc: 'db_putHex', namespace: 'db', methodName: 'putHex', params: ['db', 'key', 'value'], async: false, forbidden: true},
    {rpc: 'db_getHex', namespace: 'db', methodName: 'getHex', params: ['db', 'key'], async: false, forbidden: true},


    {rpc: 'shh_post', namespace: 'shh', methodName: 'post', params: ['postObject'], async: false, forbidden: true},
    {rpc: 'shh_version', namespace: 'shh', methodName: 'version', params: [], async: false, forbidden: true},
    {rpc: 'shh_newIdentity', namespace: 'shh', methodName: 'newIdentity', params: [], async: false, forbidden: true},
    {rpc: 'shh_hasIdentity', namespace: 'shh', methodName: 'hasIdentity', params: ['data'], async: false, forbidden: true},
    {rpc: 'shh_newGroup', namespace: 'shh', methodName: 'shh_newGroup', params: [], async: false, forbidden: true},
    {rpc: 'shh_addToGroup', namespace: 'shh', methodName: 'shh_addToGroup', params: ['data'], async: false, forbidden: true},
    {rpc: 'shh_newFilter', namespace: 'shh', methodName: 'shh_newFilter', params: ['filterOptions'], async: false, forbidden: true},
    {rpc: 'shh_uninstallFilter', namespace: 'shh', methodName: 'uninstallFilter', params: ['filterId'], async: false, forbidden: true},
    {rpc: 'shh_getFilterChanges', namespace: 'shh', methodName: 'getFilterChanges', params: ['filterId'], async: false, forbidden: true},
    {rpc: 'shh_getMessages', namespace: 'shh', methodName: 'getMessages', params: ['filterId'], async: false, forbidden: true},


    {rpc: 'personal_listAccounts', namespace: 'personal', methodName: 'listAccounts', params: [], async: false},
    {rpc: 'personal_newAccount', namespace: 'personal', methodName: 'newAccount', params: ['password'], async: false},
    {rpc: 'personal_sendTransaction', namespace: 'personal', methodName: 'sendTransaction', params: ['data'], async: false},
    {rpc: 'personal_unlockAccount', namespace: 'personal', methodName: 'unlockAccount', params: ['address', 'passphrase', 'quantity'], async: false},


];

var makeEndpointsHelpers = function () {

    var MESSAGE = '/////////////////////////////////////////////////////////////////////////////\n';
    MESSAGE += '//                                                                         //\n';
    MESSAGE += '//  This file is generated with ethereum/gen/gen-ethereum-helpers.js       //\n';
    MESSAGE += '//                                                                         //\n';
    MESSAGE += '/////////////////////////////////////////////////////////////////////////////\n\n\n';
    var shouldAllow = [];

    CODE += 'endpoint.web3 = {};\nendpoint.net = {};\nendpoint.eth = {};\nendpoint.db = {};\nendpoint.shh = {};\nendpoint.personal = {};\n\n';

    for (var i in jsonRPC) {
        var rpcData = jsonRPC[i];

        if (!rpcData.forbidden){

            CODE += '// https://github.com/ethereum/wiki/wiki/JSON-RPC#' + rpcData.rpc + '\n';
            CODE += 'endpoint';

            shouldAllow.push(rpcData.rpc);

            CODE += ((rpcData.namespace == '') ? '' : '.' + rpcData.namespace) + '.' + rpcData.methodName + ' = function(';
            if (rpcData.params.length > 0) {
                for (var i in rpcData.params) {
                    CODE += (i == 0 ? '' : ', ') + rpcData.params[i];
                }
            }
            CODE += ') {\n';

            CODE += '\tvar params = [];\n';
            for (var i in rpcData.params) {
                CODE += '\tif(' + rpcData.params[i] + ') {\n';
                CODE += '\t\tparams.push(' + rpcData.params[i] + ');\n';
                CODE += '\t}\n';
            }

            CODE += '\tvar jsonRPC = getJsonRPC("' + rpcData.rpc + '", params);\n';
            CODE += '\tvar resp = endpoint.post("", {body: jsonRPC});\n';

            if (rpcData.async) {
                CODE += '\treturn getResultAsync("' + rpcData.rpc + '", resp);\n';
            } else {
                CODE += '\treturn getResultSync("' + rpcData.rpc + '", resp);\n';
            }


            CODE += '};\n\n';

        }
    }

    var SHOULD_ALLOW = 'endpoint.shouldAllow=' + JSON.stringify(shouldAllow)+';\n\n';

    CODE = MESSAGE + SHOULD_ALLOW + CODE;

};

makeEndpointsHelpers();

fs.writeFile("../scripts/" + FILE_NAME, CODE, function (err) {
    if (err) {
        return console.error(err);
    }

    console.info('Generator has run successfully!');
});

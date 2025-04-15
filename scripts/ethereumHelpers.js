/////////////////////////////////////////////////////////////////////////////
//                                                                         //
//  This file is generated with ethereum/gen/gen-ethereum-helpers.js       //
//                                                                         //
/////////////////////////////////////////////////////////////////////////////


exports.shouldAllow=["web3_clientVersion","web3_sha3","net_version","net_peerCount","net_listening","eth_protocolVersion","eth_syncing","eth_gasPrice","eth_blockNumber","eth_getBalance","eth_getStorageAt","eth_getTransactionCount","eth_getBlockTransactionCountByHash","eth_getBlockTransactionCountByNumber","eth_getUncleCountByBlockHash","eth_getUncleCountByBlockNumber","eth_getCode","eth_sendRawTransaction","eth_call","eth_estimateGas","eth_getBlockByHash","eth_getBlockByNumber","eth_getTransactionByHash","eth_getTransactionByBlockHashAndIndex","eth_getTransactionByBlockNumberAndIndex","eth_getTransactionReceipt","eth_getUncleByBlockHashAndIndex","eth_getUncleByBlockNumberAndIndex","eth_getWork","personal_listAccounts","personal_newAccount","personal_sendTransaction","personal_unlockAccount"];

exports.web3 = {};
exports.net = {};
exports.eth = {};
exports.db = {};
exports.shh = {};
exports.personal = {};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#web3_clientVersion
exports.web3.clientVersion = function() {
	var params = [];
	var jsonRPC = getJsonRPC("web3_clientVersion", params);
	var resp = app.ethereum.post("", {body: jsonRPC});
	return getResultSync("web3_clientVersion", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#web3_sha3
exports.web3.sha3 = function(data) {
	var params = [];
	if(data) {
		params.push(data);
	}
	var jsonRPC = getJsonRPC("web3_sha3", params);
	var resp = app.ethereum.post("", {body: jsonRPC});
	return getResultSync("web3_sha3", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#net_version
exports.net.version = function() {
	var params = [];
	var jsonRPC = getJsonRPC("net_version", params);
	var resp = app.ethereum.post("", {body: jsonRPC});
	return getResultSync("net_version", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#net_peerCount
exports.net.peerCount = function() {
	var params = [];
	var jsonRPC = getJsonRPC("net_peerCount", params);
	var resp = app.ethereum.post("", {body: jsonRPC});
	return getResultSync("net_peerCount", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#net_listening
exports.net.listening = function() {
	var params = [];
	var jsonRPC = getJsonRPC("net_listening", params);
	var resp = app.ethereum.post("", {body: jsonRPC});
	return getResultSync("net_listening", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_protocolVersion
exports.eth.protocolVersion = function() {
	var params = [];
	var jsonRPC = getJsonRPC("eth_protocolVersion", params);
	var resp = app.ethereum.post("", {body: jsonRPC});
	return getResultSync("eth_protocolVersion", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_syncing
exports.eth.syncing = function() {
	var params = [];
	var jsonRPC = getJsonRPC("eth_syncing", params);
	var resp = app.ethereum.post("", {body: jsonRPC});
	return getResultSync("eth_syncing", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_gasPrice
exports.eth.gasPrice = function() {
	var params = [];
	var jsonRPC = getJsonRPC("eth_gasPrice", params);
	var resp = app.ethereum.post("", {body: jsonRPC});
	return getResultSync("eth_gasPrice", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_blockNumber
exports.eth.blockNumber = function() {
	var params = [];
	var jsonRPC = getJsonRPC("eth_blockNumber", params);
	var resp = app.ethereum.post("", {body: jsonRPC});
	return getResultSync("eth_blockNumber", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getBalance
exports.eth.getBalance = function(address, defaultBlock) {
	var params = [];
	if(address) {
		params.push(address);
	}
	if(defaultBlock) {
		params.push(defaultBlock);
	}
	var jsonRPC = getJsonRPC("eth_getBalance", params);
	var resp = app.ethereum.post("", {body: jsonRPC});
	return getResultAsync("eth_getBalance", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getStorageAt
exports.eth.getStorageAt = function(address, position, defaultBlock) {
	var params = [];
	if(address) {
		params.push(address);
	}
	if(position) {
		params.push(position);
	}
	if(defaultBlock) {
		params.push(defaultBlock);
	}
	var jsonRPC = getJsonRPC("eth_getStorageAt", params);
	var resp = app.ethereum.post("", {body: jsonRPC});
	return getResultAsync("eth_getStorageAt", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getTransactionCount
exports.eth.transactionCount = function(address, defaultBlock) {
	var params = [];
	if(address) {
		params.push(address);
	}
	if(defaultBlock) {
		params.push(defaultBlock);
	}
	var jsonRPC = getJsonRPC("eth_getTransactionCount", params);
	var resp = app.ethereum.post("", {body: jsonRPC});
	return getResultSync("eth_getTransactionCount", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getBlockTransactionCountByHash
exports.eth.getBlockTransactionCountByHash = function(data) {
	var params = [];
	if(data) {
		params.push(data);
	}
	var jsonRPC = getJsonRPC("eth_getBlockTransactionCountByHash", params);
	var resp = app.ethereum.post("", {body: jsonRPC});
	return getResultAsync("eth_getBlockTransactionCountByHash", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getBlockTransactionCountByNumber
exports.eth.getBlockTransactionCountByNumber = function(quantityOrTag) {
	var params = [];
	if(quantityOrTag) {
		params.push(quantityOrTag);
	}
	var jsonRPC = getJsonRPC("eth_getBlockTransactionCountByNumber", params);
	var resp = app.ethereum.post("", {body: jsonRPC});
	return getResultAsync("eth_getBlockTransactionCountByNumber", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getUncleCountByBlockHash
exports.eth.getUncleCountByBlockHash = function(data) {
	var params = [];
	if(data) {
		params.push(data);
	}
	var jsonRPC = getJsonRPC("eth_getUncleCountByBlockHash", params);
	var resp = app.ethereum.post("", {body: jsonRPC});
	return getResultAsync("eth_getUncleCountByBlockHash", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getUncleCountByBlockNumber
exports.eth.getUncleCountByBlockNumber = function(quantityOrTag) {
	var params = [];
	if(quantityOrTag) {
		params.push(quantityOrTag);
	}
	var jsonRPC = getJsonRPC("eth_getUncleCountByBlockNumber", params);
	var resp = app.ethereum.post("", {body: jsonRPC});
	return getResultAsync("eth_getUncleCountByBlockNumber", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getCode
exports.eth.getCode = function(address, defaultBlock) {
	var params = [];
	if(address) {
		params.push(address);
	}
	if(defaultBlock) {
		params.push(defaultBlock);
	}
	var jsonRPC = getJsonRPC("eth_getCode", params);
	var resp = app.ethereum.post("", {body: jsonRPC});
	return getResultAsync("eth_getCode", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_sendRawTransaction
exports.eth.sendRawTransaction = function(data) {
	var params = [];
	if(data) {
		params.push(data);
	}
	var jsonRPC = getJsonRPC("eth_sendRawTransaction", params);
	var resp = app.ethereum.post("", {body: jsonRPC});
	return getResultAsync("eth_sendRawTransaction", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_call
exports.eth.call = function(callObject, defaultBlock) {
	var params = [];
	if(callObject) {
		params.push(callObject);
	}
	if(defaultBlock) {
		params.push(defaultBlock);
	}
	var jsonRPC = getJsonRPC("eth_call", params);
	var resp = app.ethereum.post("", {body: jsonRPC});
	return getResultAsync("eth_call", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_estimateGas
exports.eth.estimateGas = function(ethObject) {
	var params = [];
	if(ethObject) {
		params.push(ethObject);
	}
	var jsonRPC = getJsonRPC("eth_estimateGas", params);
	var resp = app.ethereum.post("", {body: jsonRPC});
	return getResultSync("eth_estimateGas", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getBlockByHash
exports.eth.blockByHash = function(data, isReturnedFullObject) {
	var params = [];
	if(data) {
		params.push(data);
	}
	if(isReturnedFullObject) {
		params.push(isReturnedFullObject);
	}
	var jsonRPC = getJsonRPC("eth_getBlockByHash", params);
	var resp = app.ethereum.post("", {body: jsonRPC});
	return getResultSync("eth_getBlockByHash", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getBlockByNumber
exports.eth.blockByNumber = function(data, isReturnedFullObject) {
	var params = [];
	if(data) {
		params.push(data);
	}
	if(isReturnedFullObject) {
		params.push(isReturnedFullObject);
	}
	var jsonRPC = getJsonRPC("eth_getBlockByNumber", params);
	var resp = app.ethereum.post("", {body: jsonRPC});
	return getResultSync("eth_getBlockByNumber", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getTransactionByHash
exports.eth.transactionByHash = function(data) {
	var params = [];
	if(data) {
		params.push(data);
	}
	var jsonRPC = getJsonRPC("eth_getTransactionByHash", params);
	var resp = app.ethereum.post("", {body: jsonRPC});
	return getResultSync("eth_getTransactionByHash", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getTransactionByBlockHashAndIndex
exports.eth.transactionByBlockHashAndIndex = function(data, quantity) {
	var params = [];
	if(data) {
		params.push(data);
	}
	if(quantity) {
		params.push(quantity);
	}
	var jsonRPC = getJsonRPC("eth_getTransactionByBlockHashAndIndex", params);
	var resp = app.ethereum.post("", {body: jsonRPC});
	return getResultSync("eth_getTransactionByBlockHashAndIndex", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getTransactionByBlockNumberAndIndex
exports.eth.transactionByBlockNumberAndIndex = function(blockNumber, quantity) {
	var params = [];
	if(blockNumber) {
		params.push(blockNumber);
	}
	if(quantity) {
		params.push(quantity);
	}
	var jsonRPC = getJsonRPC("eth_getTransactionByBlockNumberAndIndex", params);
	var resp = app.ethereum.post("", {body: jsonRPC});
	return getResultSync("eth_getTransactionByBlockNumberAndIndex", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getTransactionReceipt
exports.eth.transactionReceipt = function(data) {
	var params = [];
	if(data) {
		params.push(data);
	}
	var jsonRPC = getJsonRPC("eth_getTransactionReceipt", params);
	var resp = app.ethereum.post("", {body: jsonRPC});
	return getResultSync("eth_getTransactionReceipt", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getUncleByBlockHashAndIndex
exports.eth.uncleByBlockHashAndIndex = function(data, quantity) {
	var params = [];
	if(data) {
		params.push(data);
	}
	if(quantity) {
		params.push(quantity);
	}
	var jsonRPC = getJsonRPC("eth_getUncleByBlockHashAndIndex", params);
	var resp = app.ethereum.post("", {body: jsonRPC});
	return getResultSync("eth_getUncleByBlockHashAndIndex", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getUncleByBlockNumberAndIndex
exports.eth.uncleByBlockNumberAndIndex = function(quantityOrTag, quantity) {
	var params = [];
	if(quantityOrTag) {
		params.push(quantityOrTag);
	}
	if(quantity) {
		params.push(quantity);
	}
	var jsonRPC = getJsonRPC("eth_getUncleByBlockNumberAndIndex", params);
	var resp = app.ethereum.post("", {body: jsonRPC});
	return getResultSync("eth_getUncleByBlockNumberAndIndex", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getWork
exports.eth.getWork = function() {
	var params = [];
	var jsonRPC = getJsonRPC("eth_getWork", params);
	var resp = app.ethereum.post("", {body: jsonRPC});
	return getResultAsync("eth_getWork", resp);
};

//////////////////////////////////////////
// Private helpers   
//////////////////////////////////////////

var getJsonRPC = function (name, params) {
    var json = {
        id: (new Date()).getTime(),
        jsonrpc: '2.0',
        method: name,
        params: params
    };
    return json;
};


var getResultSync = function (name, resp) {
    if (resp) {
        if (resp.error) {
            throw resp.error.message;
        } else if (resp.result) {
            return resp.result;
        }
    } else {
        throw 'Response is null for \'' + name + '\'';
    }
    return null
};

var getResultAsync = function (name, resp) {
    if (resp) {
        if (resp.result) {
            return resp.result;
        } else if (resp.error) {
            throw resp.error.message;
        } else {
            throw 'Callback is not defined for \'' + name + '\'';
        }

    } else {
        throw 'Response is null for \'' + name + '\'';
    }
};
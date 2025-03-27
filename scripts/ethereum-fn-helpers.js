/////////////////////////////////////////////////////////////////////////////
//                                                                         //
//  This file is generated with ethereum/gen/gen-ethereum-helpers.js       //
//                                                                         //
/////////////////////////////////////////////////////////////////////////////


endpoint.shouldAllow=["web3_clientVersion","web3_sha3","net_version","net_peerCount","net_listening","eth_protocolVersion","eth_syncing","eth_gasPrice","eth_blockNumber","eth_getBalance","eth_getStorageAt","eth_getTransactionCount","eth_getBlockTransactionCountByHash","eth_getBlockTransactionCountByNumber","eth_getUncleCountByBlockHash","eth_getUncleCountByBlockNumber","eth_getCode","eth_sendRawTransaction","eth_call","eth_estimateGas","eth_getBlockByHash","eth_getBlockByNumber","eth_getTransactionByHash","eth_getTransactionByBlockHashAndIndex","eth_getTransactionByBlockNumberAndIndex","eth_getTransactionReceipt","eth_getUncleByBlockHashAndIndex","eth_getUncleByBlockNumberAndIndex","eth_getWork","personal_listAccounts","personal_newAccount","personal_sendTransaction","personal_unlockAccount"];

endpoint.web3 = {};
endpoint.net = {};
endpoint.eth = {};
endpoint.db = {};
endpoint.shh = {};
endpoint.personal = {};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#web3_clientVersion
endpoint.web3.clientVersion = function() {
	var params = [];
	var jsonRPC = getJsonRPC("web3_clientVersion", params);
	var resp = endpoint.post("", {body: jsonRPC});
	return getResultSync("web3_clientVersion", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#web3_sha3
endpoint.web3.sha3 = function(data) {
	var params = [];
	if(data) {
		params.push(data);
	}
	var jsonRPC = getJsonRPC("web3_sha3", params);
	var resp = endpoint.post("", {body: jsonRPC});
	return getResultSync("web3_sha3", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#net_version
endpoint.net.version = function() {
	var params = [];
	var jsonRPC = getJsonRPC("net_version", params);
	var resp = endpoint.post("", {body: jsonRPC});
	return getResultSync("net_version", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#net_peerCount
endpoint.net.peerCount = function() {
	var params = [];
	var jsonRPC = getJsonRPC("net_peerCount", params);
	var resp = endpoint.post("", {body: jsonRPC});
	return getResultSync("net_peerCount", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#net_listening
endpoint.net.listening = function() {
	var params = [];
	var jsonRPC = getJsonRPC("net_listening", params);
	var resp = endpoint.post("", {body: jsonRPC});
	return getResultSync("net_listening", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_protocolVersion
endpoint.eth.protocolVersion = function() {
	var params = [];
	var jsonRPC = getJsonRPC("eth_protocolVersion", params);
	var resp = endpoint.post("", {body: jsonRPC});
	return getResultSync("eth_protocolVersion", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_syncing
endpoint.eth.syncing = function() {
	var params = [];
	var jsonRPC = getJsonRPC("eth_syncing", params);
	var resp = endpoint.post("", {body: jsonRPC});
	return getResultSync("eth_syncing", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_gasPrice
endpoint.eth.gasPrice = function() {
	var params = [];
	var jsonRPC = getJsonRPC("eth_gasPrice", params);
	var resp = endpoint.post("", {body: jsonRPC});
	return getResultSync("eth_gasPrice", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_blockNumber
endpoint.eth.blockNumber = function() {
	var params = [];
	var jsonRPC = getJsonRPC("eth_blockNumber", params);
	var resp = endpoint.post("", {body: jsonRPC});
	return getResultSync("eth_blockNumber", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getBalance
endpoint.eth.getBalance = function(address, defaultBlock) {
	var params = [];
	if(address) {
		params.push(address);
	}
	if(defaultBlock) {
		params.push(defaultBlock);
	}
	var jsonRPC = getJsonRPC("eth_getBalance", params);
	var resp = endpoint.post("", {body: jsonRPC});
	return getResultAsync("eth_getBalance", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getStorageAt
endpoint.eth.getStorageAt = function(address, position, defaultBlock) {
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
	var resp = endpoint.post("", {body: jsonRPC});
	return getResultAsync("eth_getStorageAt", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getTransactionCount
endpoint.eth.transactionCount = function(address, defaultBlock) {
	var params = [];
	if(address) {
		params.push(address);
	}
	if(defaultBlock) {
		params.push(defaultBlock);
	}
	var jsonRPC = getJsonRPC("eth_getTransactionCount", params);
	var resp = endpoint.post("", {body: jsonRPC});
	return getResultSync("eth_getTransactionCount", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getBlockTransactionCountByHash
endpoint.eth.getBlockTransactionCountByHash = function(data) {
	var params = [];
	if(data) {
		params.push(data);
	}
	var jsonRPC = getJsonRPC("eth_getBlockTransactionCountByHash", params);
	var resp = endpoint.post("", {body: jsonRPC});
	return getResultAsync("eth_getBlockTransactionCountByHash", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getBlockTransactionCountByNumber
endpoint.eth.getBlockTransactionCountByNumber = function(quantityOrTag) {
	var params = [];
	if(quantityOrTag) {
		params.push(quantityOrTag);
	}
	var jsonRPC = getJsonRPC("eth_getBlockTransactionCountByNumber", params);
	var resp = endpoint.post("", {body: jsonRPC});
	return getResultAsync("eth_getBlockTransactionCountByNumber", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getUncleCountByBlockHash
endpoint.eth.getUncleCountByBlockHash = function(data) {
	var params = [];
	if(data) {
		params.push(data);
	}
	var jsonRPC = getJsonRPC("eth_getUncleCountByBlockHash", params);
	var resp = endpoint.post("", {body: jsonRPC});
	return getResultAsync("eth_getUncleCountByBlockHash", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getUncleCountByBlockNumber
endpoint.eth.getUncleCountByBlockNumber = function(quantityOrTag) {
	var params = [];
	if(quantityOrTag) {
		params.push(quantityOrTag);
	}
	var jsonRPC = getJsonRPC("eth_getUncleCountByBlockNumber", params);
	var resp = endpoint.post("", {body: jsonRPC});
	return getResultAsync("eth_getUncleCountByBlockNumber", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getCode
endpoint.eth.getCode = function(address, defaultBlock) {
	var params = [];
	if(address) {
		params.push(address);
	}
	if(defaultBlock) {
		params.push(defaultBlock);
	}
	var jsonRPC = getJsonRPC("eth_getCode", params);
	var resp = endpoint.post("", {body: jsonRPC});
	return getResultAsync("eth_getCode", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_sendRawTransaction
endpoint.eth.sendRawTransaction = function(data) {
	var params = [];
	if(data) {
		params.push(data);
	}
	var jsonRPC = getJsonRPC("eth_sendRawTransaction", params);
	var resp = endpoint.post("", {body: jsonRPC});
	return getResultAsync("eth_sendRawTransaction", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_call
endpoint.eth.call = function(callObject, defaultBlock) {
	var params = [];
	if(callObject) {
		params.push(callObject);
	}
	if(defaultBlock) {
		params.push(defaultBlock);
	}
	var jsonRPC = getJsonRPC("eth_call", params);
	var resp = endpoint.post("", {body: jsonRPC});
	return getResultAsync("eth_call", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_estimateGas
endpoint.eth.estimateGas = function(ethObject) {
	var params = [];
	if(ethObject) {
		params.push(ethObject);
	}
	var jsonRPC = getJsonRPC("eth_estimateGas", params);
	var resp = endpoint.post("", {body: jsonRPC});
	return getResultSync("eth_estimateGas", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getBlockByHash
endpoint.eth.blockByHash = function(data, isReturnedFullObject) {
	var params = [];
	if(data) {
		params.push(data);
	}
	if(isReturnedFullObject) {
		params.push(isReturnedFullObject);
	}
	var jsonRPC = getJsonRPC("eth_getBlockByHash", params);
	var resp = endpoint.post("", {body: jsonRPC});
	return getResultSync("eth_getBlockByHash", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getBlockByNumber
endpoint.eth.blockByNumber = function(data, isReturnedFullObject) {
	var params = [];
	if(data) {
		params.push(data);
	}
	if(isReturnedFullObject) {
		params.push(isReturnedFullObject);
	}
	var jsonRPC = getJsonRPC("eth_getBlockByNumber", params);
	var resp = endpoint.post("", {body: jsonRPC});
	return getResultSync("eth_getBlockByNumber", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getTransactionByHash
endpoint.eth.transactionByHash = function(data) {
	var params = [];
	if(data) {
		params.push(data);
	}
	var jsonRPC = getJsonRPC("eth_getTransactionByHash", params);
	var resp = endpoint.post("", {body: jsonRPC});
	return getResultSync("eth_getTransactionByHash", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getTransactionByBlockHashAndIndex
endpoint.eth.transactionByBlockHashAndIndex = function(data, quantity) {
	var params = [];
	if(data) {
		params.push(data);
	}
	if(quantity) {
		params.push(quantity);
	}
	var jsonRPC = getJsonRPC("eth_getTransactionByBlockHashAndIndex", params);
	var resp = endpoint.post("", {body: jsonRPC});
	return getResultSync("eth_getTransactionByBlockHashAndIndex", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getTransactionByBlockNumberAndIndex
endpoint.eth.transactionByBlockNumberAndIndex = function(blockNumber, quantity) {
	var params = [];
	if(blockNumber) {
		params.push(blockNumber);
	}
	if(quantity) {
		params.push(quantity);
	}
	var jsonRPC = getJsonRPC("eth_getTransactionByBlockNumberAndIndex", params);
	var resp = endpoint.post("", {body: jsonRPC});
	return getResultSync("eth_getTransactionByBlockNumberAndIndex", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getTransactionReceipt
endpoint.eth.transactionReceipt = function(data) {
	var params = [];
	if(data) {
		params.push(data);
	}
	var jsonRPC = getJsonRPC("eth_getTransactionReceipt", params);
	var resp = endpoint.post("", {body: jsonRPC});
	return getResultSync("eth_getTransactionReceipt", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getUncleByBlockHashAndIndex
endpoint.eth.uncleByBlockHashAndIndex = function(data, quantity) {
	var params = [];
	if(data) {
		params.push(data);
	}
	if(quantity) {
		params.push(quantity);
	}
	var jsonRPC = getJsonRPC("eth_getUncleByBlockHashAndIndex", params);
	var resp = endpoint.post("", {body: jsonRPC});
	return getResultSync("eth_getUncleByBlockHashAndIndex", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getUncleByBlockNumberAndIndex
endpoint.eth.uncleByBlockNumberAndIndex = function(quantityOrTag, quantity) {
	var params = [];
	if(quantityOrTag) {
		params.push(quantityOrTag);
	}
	if(quantity) {
		params.push(quantity);
	}
	var jsonRPC = getJsonRPC("eth_getUncleByBlockNumberAndIndex", params);
	var resp = endpoint.post("", {body: jsonRPC});
	return getResultSync("eth_getUncleByBlockNumberAndIndex", resp);
};

// https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_getWork
endpoint.eth.getWork = function() {
	var params = [];
	var jsonRPC = getJsonRPC("eth_getWork", params);
	var resp = endpoint.post("", {body: jsonRPC});
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
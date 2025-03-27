// /////////////////////
// // Public API
// /////////////////////

function isUnit(str) {
    var units = ["wei", "kwei", "mwei", "gwei", "szabo", "finney", "ether", "kether", "mether", "gether"];
    return units.indexOf(str) > 0;
}

endpoint.utils = {};

/**
 * Checks if the given string is an address
 *
 * @method isAddress
 * @param {String} address the given HEX adress
 * @return {Boolean}
 */
endpoint.utils.isAddress = function (address) {
    if (!/^(0x)?[0-9a-f]{40}$/i.test(address)) {
// check if it has the basic requirements of an address
        return false;
    } else if (/^(0x)?[0-9a-fA-F]{40}$/.test(address)) {
// If it's all small caps or all all caps, return true
        return true;
    }
    return false;
};

endpoint.utils.getContractAddressByAlias = function (alias) {
    if (!alias) {
        throw 'Alias cannot be empty';
    }
    var res = endpoint.getContract(alias);
    if (res) {
        return res.address;
    }
    return null;
};

endpoint.utils.getContractABIByAlias = function (aliasOrAddress) {
    if (!aliasOrAddress) {
        throw 'Alias or address cannot be empty';
    }
    var res = endpoint.getContract(aliasOrAddress);
    if (res) {
        return res.abi;
    }
    return null;
};

endpoint.utils.getFunctionDefFromABI = function (fnName, aliasOrAddress) {
    var contractABI = endpoint.utils.getContractABIByAlias(aliasOrAddress);
    if (contractABI && contractABI.length) {
        for (var c in contractABI) {
            if (contractABI[c].name == fnName) {
                return contractABI[c];
            }
        }
    }
    return null;
};

endpoint.utils.processSubmittedTransaction = function (msg, res) {
    sys.storage.put("ethereum-endpoint-"+msg.options.from+'-nonce', msg.data.nonce, {ttl: 2 * 60 * 1000});
    globalUnlock(msg.options.from);
    if (msg.options.submitted) {
        var func = 'var callback = ' + msg.options.submitted + ';'
            + '\ncallback(context.msg, context.res);';
        sys.utils.script.eval(func, {msg: msg, res: res});
    }
// if there is a confirmed callback, we wait until the transaction is committed
    if (msg.options.confirmed) {
// check status of transaction
        var txOptions = {
            txHash: res.txHash
        };
        var callbackData = {
            msg: msg,
            res: res
        };
        var txCallbacks = {
            transactionConfirmed: function (receiptObj, data) {
                var msg = data.msg;
                var res = data.res;
                var receipt = receiptObj.data;
                var events = app.endpoints[msg.endpointName]._decodeLogsInReceipt(receipt);
                var func = 'var callback = ' + msg.options.confirmed + ';' +
                    '\ncallback(context.msg, context.res, context.receipt, context.events);';
                sys.utils.script.eval(func, {msg: msg, res: res, receipt: receipt, events: events});
            },
            transactionRejected: function (response, data) {
                var msg = data.msg;
                var res = data.res;
                res.errorCode = response.data.errorCode;
                delete response.errorCode;
                res.errorMessage = response.data.errorMessage;
                delete response.errorMessage;
                var func = 'var callback = ' + msg.options.error + ';' +
                    '\ncallback(context.msg, context.res, context.response);';
                sys.utils.script.eval(func, {msg: msg, res: res, response: response});
            }
        };
        if (msg.options.confirmationTimeout) {
            txOptions.confirmationTimeout = msg.options.confirmationTimeout;
        }
        if (msg.options.confirmationBlocks) {
            txOptions.confirmationBlocks = msg.options.confirmationBlocks;
        }
        if (msg.data.nonce) {
            txOptions.nonce = msg.data.nonce;
        }
        if (msg.options.from) {
            txOptions.from = msg.options.from;
        }
        app.endpoints[msg.endpointName]._confirmTransaction(txOptions, callbackData, txCallbacks);
    }
};

endpoint.utils.processDeclinedTransaction = function (msg, res) {
    sys.storage.remove("ethereum-endpoint-"+msg.options.from+"-nonce");
    globalUnlock(msg.options.from);
    if (msg.options.error) {
        if (!res) {
            res = {};
        }
        res.errorCode = 'txDeclined';
        res.errorMessage = 'Transaction was declined';
        var func = 'var callback = ' + msg.options.error + ';' +
            '\ncallback(context.msg, context.res);';
        sys.utils.script.eval(func, {msg: msg, res: res});
    }
};

endpoint.utils.processErrorTransaction = function (msg, res) {
    globalUnlock(msg.options.from);
    if (msg.options.error) {
        var func = 'var callback = ' + msg.options.error + ';' +
            '\ncallback(context.msg, context.res);';
        sys.utils.script.eval(func, {msg: msg, res: res});
    }
};

endpoint.utils.internalSendTransaction = function (options) {
    var rawTx = {
        nonce: options.nonce,
        to: options.to,
    };

    if (options.data) {
        rawTx['data'] = options.data;
    }

    if (options.value) {
        rawTx['value'] = options.value;
    }

    if (options.gasPrice) {
        rawTx['gasPrice'] = options.gasPrice;
    }

    if (options.gas) {
        rawTx['gas'] = options.gas;
    }

    if (options.from) {
        rawTx['from'] = options.from;
    }

    switch (options.signMethod) {
        case 'metamask':
            var pluginName = sys.internal.plugins.findFirstName('metamask');
            if (!pluginName) {
                throw 'Client Metamask plugin to sign transaction was not found';
            }
            sys.ui.sendMessage({
                scope: 'plugin:' + pluginName,
                name: 'sendTransaction',
                data: rawTx,
                netId: options.netId,
                endpointName: endpoint._name,
                options: options,
                callbacks: {
                    approved: function (msg, res) {
                        if (!res) {
                            sys.logs.warn('Response is empty in approve callback from ' + pluginName);
                            return;
                        }
                        app.endpoints[msg.endpointName].utils.processSubmittedTransaction(msg, res);
                    },
                    declined: function (msg, res) {
                        app.endpoints[msg.endpointName].utils.processDeclinedTransaction(msg, res);
                    },
                    error: function (msg, res) {
                        app.endpoints[msg.endpointName].utils.processErrorTransaction(msg, res);
                    }
                }
            });
            break;

        case 'managed':
            var signedRawTx;
            var msg = {
                endpointName: endpoint._name,
                data: rawTx,
                options: options
            };
            msg = JSON.parse(sys.utils.text.stringify(msg));
            if (!rawTx.gas) {
                try {
                    var estimatedGas = endpoint.eth.estimateGas(rawTx);
                    rawTx['gas'] = estimatedGas;
                } catch (e) {
                    var error = {
                        errorMessage: 'Cannot calculate gas',
                        errorCode: 'gasEstimationFail'
                    }
                    if (typeof e == 'string') {
                        error.errorMessage = error.errorMessage + ': ' + e;
                    } else if (e.message) {
                        error.errorMessage = error.errorMessage + ': ' + e.message;
                    }
                    sys.logs.warn('Cannot calculate gas', e);
                    endpoint.utils.processErrorTransaction(msg, error);
                    return;
                }
            }
            if (!rawTx.gasPrice) {
                try {
                    var gasPrice = endpoint.eth.gasPrice();
                    rawTx['gasPrice'] = gasPrice;
                } catch (e) {
                    var error = {
                        errorMessage: 'Cannot calculate gas price',
                        errorCode: 'gasEstimationFail',
                    }
                    if (typeof e == 'string') {
                        error.errorMessage = error.errorMessage + ': ' + e;
                    } else if (e.message) {
                        error.errorMessage = error.errorMessage + ': ' + e.message;
                    }
                    sys.logs.warn('Cannot calculate gas price', e);
                    endpoint.utils.processErrorTransaction(msg, error);
                    return;
                }
            }
            try {
                rawTx.netId = options.netId;
                signedRawTx = endpoint._signTransaction(rawTx);
            } catch (e) {
                var error = {
                    errorMessage: 'Cannot sign transaction with given account',
                    errorCode: 'invalidAccount'
                };
                if (typeof e == 'string') {
                    error.errorMessage = error.errorMessage + ': ' + e;
                } else if (e.message) {
                    error.errorMessage = error.errorMessage + ': ' + e.message;
                }
                sys.logs.warn('Cannot sign transaction', e);
                endpoint.utils.processErrorTransaction(msg, error);
                return;
            }
            var res;
            try {
                res = endpoint.eth.sendRawTransaction(signedRawTx.data);
            } catch (e) {
                var error = {
                    errorMessage: 'Cannot send transaction to the Ethereum network',
                    errorCode: 'invalidNetwork'
                };
                if (typeof e == 'string') {
                    error.errorMessage = error.errorMessage + ': ' + e;
                } else if (e.message) {
                    error.errorMessage = error.errorMessage + ': ' + e.message;
                }
                sys.logs.warn('Cannot send transaction to the Ethereum network', e);
                endpoint.utils.processErrorTransaction(msg, error);
                return;
            }
            endpoint.utils.processSubmittedTransaction(msg, {txHash: res});
            break;
        default:
            throw 'Unsupported sign method';
    }
};

/**
 * Compiles Solidity code returning the compiled code and ABI for each contract in the code.
 *
 * @param code the code to compile
 * @param contractName (optional) the name of the contract you want to retrieve; if empty all contracts
 *                     will be returned
 * @param libraries (optional) a map with libraries that need to be linked.
 *                  For example `{'AddressSet':'0x4025e920fb97ce003b361021534f0c0335254f65'}`
 * @returns {*} a JSON with all contracts with their compiled code and ABI. If you specified
 *              contractName, it will only return the JSON for the given contract.
 */
endpoint.compileSolidity = function (code, contractName, libraries) {
    if (!code) {
        throw 'Code cannot be empty';
    }
    var res = endpoint._compileSolidity({code: code, libraries: libraries});
    if (contractName) {
        var endsWith = function endsWith(str, suffix) {
            return str.indexOf(suffix, str.length - suffix.length) !== -1;
        };
        for (var key in res.contracts) {
            if (endsWith(key, ':' + contractName)) {
                return res.contracts[key];
            }
        }
        throw 'Contract with name [' + contractName + '] not found in code';
    } else {
        return res.contracts;
    }
};

/**
 * Encodes a function call with their parameters.
 *
 * @param aliasOrAddress the contract's alias or address
 * @param fnName name of the function in the contract
 * @param params an array with the params of the function
 * @returns {*} a string with the encoded function call
 */
endpoint.encodeFunction = function (aliasOrAddress, fnName, params) {
    params = params || [];
    var functionAbiDef = endpoint.utils.getFunctionDefFromABI(fnName, aliasOrAddress);
    if (!functionAbiDef) {
        throw 'Cannot find function [' + fnName + '] in ABI';
    }
    var data;
    try {
        data = endpoint._encodedFunction({fnAbi: functionAbiDef, params: params});
    } catch (e) {
        throw 'There was a problem encoding params: ' + sys.exceptions.getMessage(e) + '. Code: ' + sys.exceptions.getCode(e);
    }
    return data;
};

/**
 * Calls a view function in a contract (it doesn't change the state of the blockchain) and returns
 * the response.
 *
 * @param aliasOrAddress the alias or address of the contract in the app
 * @param fnName the name of the function to execute
 * @param params an array with the params of the function
 * @param fromAddress origin account address; optional
 * @returns {*} an array with the response of the function
 */
endpoint.callFunction = function (aliasOrAddress, fnName, params, fromAddress) {
    params = params || [];
    var functionAbiDef = endpoint.utils.getFunctionDefFromABI(fnName, aliasOrAddress);
    if (!functionAbiDef) {
        throw 'Cannot find function [' + fnName + '] in ABI';
    }
    var data;
    try {
        data = endpoint._encodedFunction({fnAbi: functionAbiDef, params: params});
    } catch (e) {
        throw 'There was a problem encoding params: ' + sys.exceptions.getMessage(e) + '. Code: ' + sys.exceptions.getCode(e);
    }
    if (functionAbiDef['stateMutability'] !== 'view') {
        throw 'The function [' + fnName + '] is not a view. Use sendTransaction() instead.';
    }

    var callObject = {
        from: fromAddress,
        to: endpoint.utils.isAddress(aliasOrAddress) ? aliasOrAddress : endpoint.utils.getContractAddressByAlias(aliasOrAddress),
        data: data
    };
    try {
        var data = endpoint.eth.call(callObject, 'latest');
        var decodedData;
        try {
            decodedData = endpoint._decodeFunction({fnAbi: functionAbiDef, data: data});
        } catch (e) {
            sys.logs.error('Error decoding response of function [' + fnName + '] in contract [' + aliasOrAddress + ']', e);
            throw 'There was a problem decoding data returned by function [' + fnName + '] in contract [' + aliasOrAddress + ']';
        }
        return decodedData;
    } catch (err) {
        sys.logs.error('Error calling function [' + fnName + '] in contract [' + aliasOrAddress + ']', err);
        throw 'There was an error calling function [' + fnName + '] in contract [' + aliasOrAddress + ']';
    }
};

/**
 * Estimates the needed gas to make a transaction. Keep in mind that this is just an estimation and real
 * gas when the transaction is executed might be different.
 *
 * @param aliasOrAddress the alias or address of the contract in the app
 * @param fnName the name of the function to execute
 * @param params an array with the params of the function
 * @param fromAddress origin account address
 */
endpoint.estimateTransaction = function (aliasOrAddress, fnName, params, fromAddress) {
    var rawTx = {
        to: endpoint.utils.isAddress(aliasOrAddress) ? aliasOrAddress : endpoint.utils.getContractAddressByAlias(aliasOrAddress),
        from: fromAddress
    };
    var functionAbiDef = endpoint.utils.getFunctionDefFromABI(fnName, aliasOrAddress);
    if (!functionAbiDef) {
        throw 'Cannot find function [' + fnName + '] in ABI';
    }
    var data;
    try {
        data = endpoint._encodedFunction({fnAbi: functionAbiDef, params: params});
    } catch (e) {
        throw 'There was a problem encoding params: ' + sys.exceptions.getMessage(e) + '. Code: ' + sys.exceptions.getCode(e);
    }
    rawTx.data = data;
    var estimatedGas = endpoint.eth.estimateGas(rawTx);
    return estimatedGas;
};

/**
 * Sends a transaction to the Ethereum network. It will sign the transaction using the given sign
 * method (which could involved a UI plugin, like MetaMask), send it to the network and call
 * callbacks as events happen.
 *
 * @param aliasOrAddress the alias or address of the contract in the app
 * @param fnName the name of the function to execute
 * @param params an array with the params of the function
 * @param fromAddress origin account address
 * @param signMethod the method used to sign the transaction, which is the name of the plugin, like 'metamask'
 * @param options other Ethereum parameters needed for transaction like gas, gasPrice,
 *                and callbacks: submitted, confirmed, error.
 */
endpoint.sendTransaction = function (aliasOrAddress, fnName, params, fromAddress, signMethod, options) {
    globalLock(fromAddress);
    try {
        options = options || {};
        params = params || [];
        var functionAbiDef = endpoint.utils.getFunctionDefFromABI(fnName, aliasOrAddress);
        if (!functionAbiDef) {
            throw 'Cannot find function [' + fnName + '] in ABI';
        }
        var data;
        try {
            data = endpoint._encodedFunction({fnAbi: functionAbiDef, params: params});
        } catch (e) {
            throw 'There was a problem encoding params: ' + sys.exceptions.getMessage(e) + '. Code: ' + sys.exceptions.getCode(e);
        }
        if (!fromAddress) {
            throw 'Address must be specified for this call.';
        }
        if (!signMethod) {
            throw 'Sign method must be specified for this call.';
        }
        if (functionAbiDef['stateMutability'] === 'view') {
            throw 'This function is a view. Use the method callFunction() instead.';
        }
        if (!options.nonce) {
            options.nonce = getNonce(fromAddress);
        }
        options.to = endpoint.utils.isAddress(aliasOrAddress) ? aliasOrAddress : endpoint.utils.getContractAddressByAlias(aliasOrAddress);
        options.data = data;
        options.netId = endpoint.net.version();
        options.from = fromAddress;
        options.signMethod = signMethod;
        endpoint.utils.internalSendTransaction(options);
    } catch (e) {
        globalUnlock(fromAddress);
        throw e;
    }
};

/**
 * Sends a transaction to send ether to the Ethereum network. It will sign the transaction using the given sign
 * method (which could involved a UI plugin, like MetaMask), send it to the network and call callbacks as events
 * happen.
 *
 * @param aliasOrAddress the alias or address to send the ether
 * @param amount the amount of ether to send
 * @param fromAddress origin account address
 * @param signMethod the method used to sign the transaction, which is the name of the plugin, like 'metamask'
 * @param options other Ethereum parameters needed for transaction like gas, gasPrice,
 *                and callbacks: submitted, confirmed, error.
 */
endpoint.sendEther = function (aliasOrAddress, amount, fromAddress, signMethod, options) {
    globalLock(fromAddress);
    try {
        options = options || {};
        if (!fromAddress) {
            throw 'Address must be specified for this call.';
        }
        if (!signMethod) {
            throw 'Sign method must be specified for this call.';
        }
        if (!options.nonce) {
            options.nonce = getNonce(fromAddress);
        }
        options.to = endpoint.utils.isAddress(aliasOrAddress) ? aliasOrAddress : endpoint.utils.getContractAddressByAlias(aliasOrAddress);
        options.value = amount;
        options.netId = endpoint.net.version();
        options.from = fromAddress;
        options.signMethod = signMethod;
        endpoint.utils.internalSendTransaction(options);
    } catch (e) {
        globalUnlock(fromAddress);
        throw e;
    }
};

/**
 * Deployes a contract in the Ethereum network.
 *
 * @param alias the alias to use for the new contract. If it is null is going to be set address of created contract.
 * @param compiledCode the compile code of the contract
 * @param abi the ABI of the contract
 * @param fromAddress origin account address
 * @param signMethod the method used to sign the
 * @param options other Ethereum parameters needed for transaction like gas, gasPrice,
 *                and callbacks: submitted, confirmed, error.
 */
endpoint.createContract = function (alias, compiledCode, abi, fromAddress, signMethod, options) {
    globalLock(fromAddress);
    try {
        if (alias && endpoint.getContract(alias)) {
            throw 'There is another contract with alias [' + alias + ']';
        }
        if (!compiledCode) {
            throw 'Compiled code cannot be empty';
        }
        if (!abi) {
            throw 'ABI cannot be empty';
        }
        if (!fromAddress) {
            throw 'Address must be specified for this call.';
        }
        options = options || {};
        if (!options.nonce) {
            options.nonce = getNonce(fromAddress);
        }
        options.netId = endpoint.net.version();
        options.from = fromAddress;
        options.signMethod = signMethod;
        options.originalConfirmedCallback = options.confirmed;
        options.contractInfo = {
            alias: alias,
            abi: abi
        };
        if (compiledCode.indexOf('0x') != 0) {
            compiledCode = '0x' + compiledCode;
        }
        options.data = compiledCode;
        options.confirmed = function (msg, res, receipt) {
            app.endpoints[msg.endpointName]._registerContract({
                alias: msg.options.contractInfo.alias ? msg.options.contractInfo.alias : receipt.contractAddress,
                abi: JSON.parse(msg.options.contractInfo.abi),
                address: receipt.contractAddress
            });
            var func = 'var callback = ' + msg.options.originalConfirmedCallback + '; callback(context.msg, context.res, context.receipt);';
            sys.utils.script.eval(func, {msg: msg, res: res, receipt: receipt});
        };
        endpoint.utils.internalSendTransaction(options);
    } catch (e) {
        globalUnlock(fromAddress);
        throw e;
    }
};

/**
 * Registers an existing contract in the endpoint.
 *
 * @param alias the alias for this contract (optional)
 * @param contractAddress the Ethereum address of the contract
 * @param abi the ABI of the contract (string)
 */
endpoint.registerContract = function (alias, contractAddress, abi) {
    if (!contractAddress) {
        throw 'Address is required';
    }
    if (!endpoint.utils.isAddress(contractAddress)) {
        throw 'Invalid address';
    }
    if (!abi) {
        throw 'ABI is required';
    }
    if (!alias) {
        alias = contractAddress;
    }

    endpoint._registerContract({
        alias: alias,
        address: contractAddress,
        abi: JSON.parse(abi)
    });
};

/**
 * Returns contract by alias.
 *
 * @param aliasOrAddress the alias or address of the contract
 * @returns {*} the JSON of the contract
 */
endpoint.getContract = function (aliasOrAddress) {
    if (!aliasOrAddress) {
        throw 'Alias or address cannot be empty';
    }
    var isAddress = false;
    if (endpoint.utils.isAddress(aliasOrAddress)) {
        isAddress = true;
    }
    var contract = endpoint._getContract({aliasOrAddress: aliasOrAddress, isAddress: isAddress});
    if (contract && contract.alias) {
        return contract;
    }
    return null;
};

/**
 * Removes a contract by alias. Only works for contracts added using registerContract().
 *
 * @param aliasOrAddress the alias or address of the contract to remove
 * @returns {*} the contract removed
 */
endpoint.removeContract = function (aliasOrAddress) {
    if (!aliasOrAddress) {
        throw 'Alias or address cannot be empty';
    }
    var isAddress = false;
    if (endpoint.utils.isAddress(aliasOrAddress)) {
        isAddress = true;
    }
    return endpoint._removeContract({alias: aliasOrAddress, isAddress: isAddress});
};

/**
 * Create a new managed account with its private key.
 *
 * @returns {string} the address of the created account
 */
endpoint.createAccount = function () {
    var res = endpoint._createAccount({});
    return endpoint.toChecksumAddress(res.address);
};

/**
 * Imports an account by providing the private key in hexadecimal. Throws an exception if the private key
 * already exists.
 *
 * @param privateKey the private key in hexadecimal
 * @returns {string} the address of the new account
 */
endpoint.importAccount = function (privateKey) {
    if (!privateKey) {
        throw 'Private key cannot be empty';
    }
    var res = endpoint._importAccount({privateKey: privateKey});
    return endpoint.toChecksumAddress(res.address);
};

/**
 * Exports the private key of the account. You shouldn't do it except that it is a sample test key.
 *
 * @param address the address of the key to export
 * @returns {*} a map with fields `address` and `privateKey`
 */
endpoint.exportAccount = function (address) {
    if (!address) {
        throw 'Address cannot be empty';
    }
    var res = endpoint._exportAccount({address: address});
    res.address = endpoint.toChecksumAddress(res.address);
    return res;
};

/**
 * Gets the checksum address.
 *
 * @param address to convert
 * @returns {string} converted address
 */
endpoint.toChecksumAddress = function (address) {
    address = address.toLowerCase().replace('0x', '');
    var hash = sys.utils.crypto.keccak(address).toString('hex');
    var ret = '0x';

    for (var i = 0; i < address.length; i++) {
        if (parseInt(hash[i], 16) >= 8) {
            ret += address[i].toUpperCase();
        } else {
            ret += address[i];
        }
    }
    return ret
};

/**
 * Returns the network the endpoint is connected to.
 */
endpoint.getNetwork = function () {
    return endpoint._configuration.networkUrl;
};

///////////////////////////////////////
// Public API - Generic Functions
///////////////////////////////////////

endpoint.post = function (url, options) {
    options = checkHttpOptions(url, options);
    return endpoint._post(options);
};

var _overridePost = endpoint._post;
endpoint._post = function (options) {
    var body = (options && options.body) ? options.body : false;
    if (!body || !endpoint.shouldAllow || endpoint.shouldAllow.indexOf(body.method) < 0) {
        throw 'Forbidden ' + body.method;
    }
    return _overridePost(options);
};

//////////////////////////////////////
//  Private helpers
/////////////////////////////////////

var checkHttpOptions = function (url, options) {
    options = options || {};
    if (!!url) {
        if (isObject(url)) {
            // take the 'url' parameter as the options
            options = url || {};
        } else {
            if (!!options.path || !!options.params || !!options.body) {
                // options contains the http package format
                options.path = url;
            } else {
                // create html package
                options = {
                    path: url,
                    body: options
                }
            }
        }
    }
    return options;
};

var isObject = function (obj) {
    return !!obj && stringType(obj) === '[object Object]'
};

var stringType = Function.prototype.call.bind(Object.prototype.toString);

function globalLock(key, timeout) {
    if (!timeout) timeout = 5000 * 60;
    var timeSpent = 0;
    while (!sys.storage.putIfAbsent(key, key, {ttl: 10 * 60 * 1000}) && timeSpent < timeout) {
        sys.utils.script.wait(100);
        timeSpent += 100;
    }
    if (timeSpent > timeout)
        throw 'The lock is spending more time than the given timeout';
}

function globalUnlock(key) {
    sys.storage.remove(key);
}

function getNonce(address) {
    var lastNonce = sys.storage.get("ethereum-endpoint-"+address+'-nonce');
    if (lastNonce) {
        var newNonce = parseInt(lastNonce) + 1;
        return '0x'+newNonce.toString(16);
    } else {
        return endpoint.eth.transactionCount(address, 'pending');
    }
}
// /////////////////////
// // Public API
// /////////////////////

function isUnit(str) {
    var units = ["wei", "kwei", "mwei", "gwei", "szabo", "finney", "ether", "kether", "mether", "gether"];
    return units.indexOf(str) > 0;
}

exports.utils = {};

/**
 * Checks if the given string is an address
 *
 * @method isAddress
 * @param {String} address the given HEX adress
 * @return {Boolean}
 */
exports.utils.isAddress = function (address) {
    if (!/^(0x)?[0-9a-f]{40}$/i.test(address)) {
// check if it has the basic requirements of an address
        return false;
    } else if (/^(0x)?[0-9a-fA-F]{40}$/.test(address)) {
// If it's all small caps or all all caps, return true
        return true;
    }
    return false;
};

exports.utils.getContractAddressByAlias = function (alias) {
    if (!alias) {
        throw 'Alias cannot be empty';
    }
    var res = app.ethereum.getContract(alias);
    if (res) {
        return res.address;
    }
    return null;
};

exports.utils.getContractABIByAlias = function (aliasOrAddress) {
    if (!aliasOrAddress) {
        throw 'Alias or address cannot be empty';
    }
    var res = app.ethereum.getContract(aliasOrAddress);
    if (res) {
        return res.abi;
    }
    return null;
};

exports.utils.getFunctionDefFromABI = function (fnName, aliasOrAddress) {
    var contractABI = app.ethereum.utils.getContractABIByAlias(aliasOrAddress);
    if (contractABI && contractABI.length) {
        for (var c in contractABI) {
            if (contractABI[c].name == fnName) {
                return contractABI[c];
            }
        }
    }
    return null;
};

exports.utils.processSubmittedTransaction = function (msg, res) {
    sys.storage.put("ethereum-service-"+msg.options.from+'-nonce', msg.data.nonce, {ttl: 2 * 60 * 1000});
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
                var events = svc.ethereum.decodeLogsInReceipt(receipt);
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
        svc.ethereum.confirmTransaction(txOptions, callbackData, txCallbacks);
    }
};

exports.utils.processDeclinedTransaction = function (msg, res) {
    sys.storage.remove("ethereum-service>-"+msg.options.from+"-nonce");
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

exports.utils.processErrorTransaction = function (msg, res) {
    globalUnlock(msg.options.from);
    if (msg.options.error) {
        var func = 'var callback = ' + msg.options.error + ';' +
            '\ncallback(context.msg, context.res);';
        sys.utils.script.eval(func, {msg: msg, res: res});
    }
};

exports.utils.internalSendTransaction = function (options) {
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
            if (!pkg.metamask) {
                throw 'Metamask package was not found in the app. Must be installed and connected.';
            }
            sys.ui.sendMessage({
                scope: 'uiService:metamask.metaMask',
                name: 'sendTransaction',
                data: rawTx,
                netId: options.netId,
                //serviceName: endpoint._name,
                options: options,
                callbacks: {
                    approved: function (msg, res) {
                        if (!res) {
                            sys.logs.warn('Response is empty in approve callback from metamask');
                            return;
                        }
                        svc.ethereum.utils.processSubmittedTransaction(msg, res);
                    },
                    declined: function (msg, res) {
                        svc.ethereum.utils.processDeclinedTransaction(msg, res);
                    },
                    error: function (msg, res) {
                        svc.ethereum.utils.processErrorTransaction(msg, res);
                    }
                }
            });
            break;

        case 'managed':
            var signedRawTx;
            var msg = {
                //endpointName: endpoint._name,
                data: rawTx,
                options: options
            };
            msg = JSON.parse(sys.utils.text.stringify(msg));
            if (!rawTx.gas) {
                try {
                    var estimatedGas = app.ethereumHelpers.eth.estimateGas(rawTx);
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
                    app.ethereum.utils.processErrorTransaction(msg, error);
                    return;
                }
            }
            if (!rawTx.gasPrice) {
                try {
                    var gasPrice = app.ethereumHelpers.eth.gasPrice();
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
                    app.ethereum.utils.processErrorTransaction(msg, error);
                    return;
                }
            }
            try {
                rawTx.netId = options.netId;
                signedRawTx = svc.ethereum.signTransaction(rawTx);
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
                app.ethereum.utils.processErrorTransaction(msg, error);
                return;
            }
            var res;
            try {
                res = app.ethereumHelpers.eth.sendRawTransaction(signedRawTx.data);
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
                app.ethereum.utils.processErrorTransaction(msg, error);
                return;
            }
            app.ethereum.utils.processSubmittedTransaction(msg, {txHash: res});
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
exports.compileSolidity = function (code, contractName, libraries) {
    if (!code) {
        throw 'Code cannot be empty';
    }
    var res = svc.ethereum.compileSolidity({code: code, libraries: libraries});
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
exports.encodeFunction = function (aliasOrAddress, fnName, params) {
    params = params || [];
    var functionAbiDef = app.ethereum.utils.getFunctionDefFromABI(fnName, aliasOrAddress);
    if (!functionAbiDef) {
        throw 'Cannot find function [' + fnName + '] in ABI';
    }
    var data;
    try {
        data = svc.ethereum.encodedFunction({fnAbi: functionAbiDef, params: params});
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
exports.callFunction = function (aliasOrAddress, fnName, params, fromAddress) {
    params = params || [];
    var functionAbiDef = app.ethereum.utils.getFunctionDefFromABI(fnName, aliasOrAddress);
    if (!functionAbiDef) {
        throw 'Cannot find function [' + fnName + '] in ABI';
    }
    var data;
    try {
        data = svc.ethereum.encodedFunction({fnAbi: functionAbiDef, params: params});
    } catch (e) {
        throw 'There was a problem encoding params: ' + sys.exceptions.getMessage(e) + '. Code: ' + sys.exceptions.getCode(e);
    }
    if (functionAbiDef['stateMutability'] !== 'view') {
        throw 'The function [' + fnName + '] is not a view. Use sendTransaction() instead.';
    }

    var callObject = {
        from: fromAddress,
        to: app.ethereum.utils.isAddress(aliasOrAddress) ? aliasOrAddress : app.ethereum.utils.getContractAddressByAlias(aliasOrAddress),
        data: data
    };
    try {
        var data = app.ethereumHelpers.eth.call(callObject, 'latest');
        var decodedData;
        try {
            decodedData = svc.ethereum.decodeFunction({fnAbi: functionAbiDef, data: data});
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
exports.estimateTransaction = function (aliasOrAddress, fnName, params, fromAddress) {
    var rawTx = {
        to: app.ethereum.utils.isAddress(aliasOrAddress) ? aliasOrAddress : app.ethereum.utils.getContractAddressByAlias(aliasOrAddress),
        from: fromAddress
    };
    var functionAbiDef = app.ethereum.utils.getFunctionDefFromABI(fnName, aliasOrAddress);
    if (!functionAbiDef) {
        throw 'Cannot find function [' + fnName + '] in ABI';
    }
    var data;
    try {
        data = svc.ethereum.encodedFunction({fnAbi: functionAbiDef, params: params});
    } catch (e) {
        throw 'There was a problem encoding params: ' + sys.exceptions.getMessage(e) + '. Code: ' + sys.exceptions.getCode(e);
    }
    rawTx.data = data;
    var estimatedGas = app.ethereumHelpers.eth.estimateGas(rawTx);
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
exports.sendTransaction = function (aliasOrAddress, fnName, params, fromAddress, signMethod, options) {
    globalLock(fromAddress);
    try {
        options = options || {};
        params = params || [];
        var functionAbiDef = app.ethereum.utils.getFunctionDefFromABI(fnName, aliasOrAddress);
        if (!functionAbiDef) {
            throw 'Cannot find function [' + fnName + '] in ABI';
        }
        var data;
        try {
            data = svc.ethereum.encodedFunction({fnAbi: functionAbiDef, params: params});
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
        options.to = app.ethereum.utils.isAddress(aliasOrAddress) ? aliasOrAddress : app.ethereum.utils.getContractAddressByAlias(aliasOrAddress);
        options.data = data;
        options.netId = app.ethereumHelpers.eth.net.version();
        options.from = fromAddress;
        options.signMethod = signMethod;
        app.ethereum.utils.internalSendTransaction(options);
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
exports.sendEther = function (aliasOrAddress, amount, fromAddress, signMethod, options) {
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
        options.to = app.ethereum.utils.isAddress(aliasOrAddress) ? aliasOrAddress : app.ethereum.utils.getContractAddressByAlias(aliasOrAddress);
        options.value = amount;
        options.netId = app.ethereumHelpers.eth.net.version();
        options.from = fromAddress;
        options.signMethod = signMethod;
        app.ethereum.utils.internalSendTransaction(options);
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
exports.createContract = function (alias, compiledCode, abi, fromAddress, signMethod, options) {
    globalLock(fromAddress);
    try {
        if (alias && app.ethereum.getContract(alias)) {
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
        options.netId = app.ethereumHelpers.eth.net.version();
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
            svc.ethereum.registerContract({
                alias: msg.options.contractInfo.alias ? msg.options.contractInfo.alias : receipt.contractAddress,
                abi: JSON.parse(msg.options.contractInfo.abi),
                address: receipt.contractAddress
            });
            var func = 'var callback = ' + msg.options.originalConfirmedCallback + '; callback(context.msg, context.res, context.receipt);';
            sys.utils.script.eval(func, {msg: msg, res: res, receipt: receipt});
        };
        app.ethereum.utils.internalSendTransaction(options);
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
exports.registerContract = function (alias, contractAddress, abi) {
    if (!contractAddress) {
        throw 'Address is required';
    }
    if (!app.ethereum.utils.isAddress(contractAddress)) {
        throw 'Invalid address';
    }
    if (!abi) {
        throw 'ABI is required';
    }
    if (!alias) {
        alias = contractAddress;
    }

    svc.ethereum.registerContract({
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
exports.getContract = function (aliasOrAddress) {
    if (!aliasOrAddress) {
        throw 'Alias or address cannot be empty';
    }
    var isAddress = false;
    if (app.ethereum.utils.isAddress(aliasOrAddress)) {
        isAddress = true;
    }
    var contract = svc.ethereum.getContract({aliasOrAddress: aliasOrAddress, isAddress: isAddress});
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
exports.removeContract = function (aliasOrAddress) {
    if (!aliasOrAddress) {
        throw 'Alias or address cannot be empty';
    }
    var isAddress = false;
    if (app.ethereum.utils.isAddress(aliasOrAddress)) {
        isAddress = true;
    }
    return svc.ethereum.removeContract({alias: aliasOrAddress, isAddress: isAddress});
};

/**
 * Create a new managed account with its private key.
 *
 * @returns {string} the address of the created account
 */
exports.createAccount = function () {
    var res = svc.ethereum.createAccount({});
    return app.ethereum.toChecksumAddress(res.address);
};

/**
 * Imports an account by providing the private key in hexadecimal. Throws an exception if the private key
 * already exists.
 *
 * @param privateKey the private key in hexadecimal
 * @returns {string} the address of the new account
 */
exports.importAccount = function (privateKey) {
    if (!privateKey) {
        throw 'Private key cannot be empty';
    }
    var res = svc.ethereum.importAccount({privateKey: privateKey});
    return app.ethereum.toChecksumAddress(res.address);
};

/**
 * Exports the private key of the account. You shouldn't do it except that it is a sample test key.
 *
 * @param address the address of the key to export
 * @returns {*} a map with fields `address` and `privateKey`
 */
exports.exportAccount = function (address) {
    if (!address) {
        throw 'Address cannot be empty';
    }
    var res = svc.ethereum.exportAccount({address: address});
    res.address = app.ethereum.toChecksumAddress(res.address);
    return res;
};

/**
 * Gets the checksum address.
 *
 * @param address to convert
 * @returns {string} converted address
 */
exports.toChecksumAddress = function (address) {
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
exports.getNetwork = function () {
    return svc.ethereum.configuration.networkUrl;
};

///////////////////////////////////////
// Public API - Generic Functions
///////////////////////////////////////

exports.post = function (url, options) {
    options = checkHttpOptions(url, options);
    return app.ethereum._post(options);
};

exports._post = function (options) {
    var body = (options && options.body) ? options.body : false;
    if (!body || !app.ethereumHelpers.eth.shouldAllow || app.ethereumHelpers.eth.shouldAllow.indexOf(body.method) < 0) {
        throw 'Forbidden ' + body.method;
    }
    return svc.ethereum.post(options);
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
    var lastNonce = sys.storage.get("ethereum-service-"+address+'-nonce');
    if (lastNonce) {
        var newNonce = parseInt(lastNonce) + 1;
        return '0x'+newNonce.toString(16);
    } else {
        return app.ethereumHelpers.eth.transactionCount(address, 'pending');
    }
}
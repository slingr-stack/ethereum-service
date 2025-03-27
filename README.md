---
title: Ethereum endpoint
keywords: 
last_updated: April 26, 2018
tags: []
summary: "Detailed description of how the Ethereum endpoint works and its configuration."
---

## Overview

The Ethereum endpoint allows to interact with the Ethereum blockchain. This is the list of features:

- Helpers to simplify calls to functions in smart contracts
- Handling of events fired by smart contracts
- Integration with wallets like MetaMask
- Management of private keys
- Creation of smart contracts (compilation and deployment)
- Access to the [JSON RPC API](https://github.com/ethereum/wiki/wiki/JSON-RPC)
- Collection of helpers which contain specific functionality for the Ethereum ecosystem.

It is important to look at [JSON RPC API](https://github.com/ethereum/wiki/wiki/JSON-RPC) as most of
the Javascript API of the endpoint is based on it.

## Quick Start

Given following contract 

```
contract Mortal {
    /* Define variable owner of the type address */
    address owner;

    /* This function is executed at initialization and sets the owner of the contract */
    function Mortal() { owner = msg.sender; }

    /* Function to recover the funds on the contract */
    function kill() { if (msg.sender == owner) selfdestruct(owner); }
}

contract Greeter is Mortal {
    /* Define variable greeting of the type string */
    string greeting;

    event NewGreeting(
       string greeting
    );

    /* This runs when the contract is executed */
    function setGreeting(string _greeting) public {
        greeting = _greeting;
        NewGreeting(_greeting);
    }

    /* Main function */
    function getGreeting() constant returns (string) {
        return greeting;
    }
}
```

With [Remix](https://remix.ethereum.org/) extract the ABI definition

```json
[
	{
		"constant": false,
		"inputs": [],
		"name": "kill",
		"outputs": [],
		"payable": false,
		"stateMutability": "nonpayable",
		"type": "function"
	},
	{
		"constant": false,
		"inputs": [
			{
				"name": "_greeting",
				"type": "string"
			}
		],
		"name": "setGreeting",
		"outputs": [],
		"payable": false,
		"stateMutability": "nonpayable",
		"type": "function"
	},
	{
		"constant": true,
		"inputs": [],
		"name": "getGreeting",
		"outputs": [
			{
				"name": "",
				"type": "string"
			}
		],
		"payable": false,
		"stateMutability": "view",
		"type": "function"
	},
	{
		"anonymous": false,
		"inputs": [
			{
				"indexed": false,
				"name": "greeting",
				"type": "string"
			}
		],
		"name": "NewGreeting",
		"type": "event"
	}
]
```

In endpoint configuration set contract with following information:

- the **address of deployed contract** for example `0x692a70d2e424a56d2c6c27aa97d1a86395877b3a`
- the **aliasOrAddress** for example `Greeter` or the address of deployed contract.
- **ABI defintion** described above

> Remember set user configurations before execute a function

In order to run **setGreeting** in smart contract we fire next code. After that Metamask is opened to confirm the transaction unless the method was of view type, in this case it is executed directly.

```javascript
app.endpoints.ethereum.sendTransaction(
  'Greeter', // contract alias  or the address of deployed contract
  'setGreeting', // function name
  ['Hello'], // params
  '0x590782dc744cb95662192bde0da32acf5e99d851', // from address
  'metamask', // sign method
  { // callbacks
    submitted: function(msg, res) {
      sys.logs.info('*** tx submitted');
      sys.logs.info('*** msg: '+JSON.stringify(msg));
      sys.logs.info('*** res: '+JSON.stringify(res));
    },
    confirmed: function(msg, res, receipt) {
      sys.logs.info('*** tx confirmed');
      sys.logs.info('*** msg: '+JSON.stringify(msg));
      sys.logs.info('*** res: '+JSON.stringify(res));
      sys.logs.info('*** receipt: '+JSON.stringify(receipt));
    },
    error: function(msg, res, receipt) {
      sys.logs.info('*** tx error');
      sys.logs.info('*** msg: '+JSON.stringify(msg));
      sys.logs.info('*** res: '+JSON.stringify(res));
      if (receipt) {
        sys.logs.info('*** receipt: '+JSON.stringify(receipt));
      }
    }
  }
);
```

When transaction is mined successfully the response is:

```json
{
  "tx": {
    "nonce": "0x1",
    "to": "0xbfe45572b9983fc622cefbdc8899f2c78ccac9b4",
    "data": "0xa41368620000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000648656c6c6f320000000000000000000000000000000000000000000000000000",
    "from": "0xff8b4c31d78ff1f3b44f911cb769eb8d2cf38154"
  },
  "txHash": "0x6f581d15041db34ddddc9153a83ac35f36622156dbdb206d449ca9bc74c9e5c5"
}
```

And event is fired with following information

```json
{
  "type": "ENDPOINT",
  "endpoint": "ethereum",
  "endpointEvent": "contractEvent",
  "date": 1524510669665,
  "data": {
    "result": {
      "address": "0xbfe45572b9983fc622cefbdc8899f2c78ccac9b4",
      "topics": [
        "0x0d332ed5c7d6f1999116748c0eb99c740f276d879d025a5be6435fcf177785de"
      ],
      "data": "0x0000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000648656c6c6f320000000000000000000000000000000000000000000000000000",
      "blockNumber": "0x22",
      "transactionHash": "0x6f581d15041db34ddddc9153a83ac35f36622156dbdb206d449ca9bc74c9e5c5",
      "transactionIndex": "0x0",
      "blockHash": "0xb0ed0e3fec7edefdd5b2a99d50da94c01b0b4a7e2a4e2c5c7802e724fdd2ac0b",
      "logIndex": "0x0",
      "removed": false
    },
    "output": [
      [
        "Hello2"
      ]
    ]
  }
}
```

## Configuration

### Service

This is the service that will be used to connect to Ethereum. These are the options:

- `Infura`: you will need to setup an account at [infura.io](http://infura.io) and that will let you access the
  Ethereum network without having to setup any client node. You will need to provide your API key and secret.
- `Custom': in this case you will need to setup an Ethereum node that it is publicly accessible. This node needs
  to have the JSON-RPC API enabled as well as websockets. For example if you are using `geth` you need to use
  something like this to start it: 
  ```
  --rpc --rpcapi "eth,web3,personal,net" --rpcport "8545" --rpcaddr "10.240.0.7" --rpccorsdomain "*" --ws --wsaddr "10.240.0.7" --wsorigins "*" 
  ```


### Infura network

This is the Ethereum network to connect to. Allowed values are `mainnet`, `ropsten`, `kovan` and `rinkeby`.

### Infura API key

The API key of your Infura account.

### Infura API secret

The API secret of your Infura account.

### Custom node URL

The URL of your custom node. This is the URL for the JSON-RPC API.

### Custom node WS URL

The URL of your custom node. This is the URL for WebSockets.

### Contracts

This a list of contracts in the Ethereum network that will be used by your app. These contracts have 
to be already deployed in the network and you must have the address and the ABI.
 
So each contract has the following settings:

- **Address**: the address of the smart contract to call. This is where contract was deployed.
- **Alias**: alias of the contract that will be used to to reference the contract easily without 
  having to type the address. It can be `null` and in that case works with contract address.
- **ABI definition**: ABI (Application Binary Interface) of the contract that is generated when
  compiling it. It is needed to encode function calls and decode responses and events.

### Confirmation timeout

This is the number of seconds to wait for the confirmation to arrive. By default is 30 minutes.

### Confirmation blocks

This is the number of confirmed blocks after the transaction has been processed. 
This is to make sure that the transaction cannot be rolled back later. By default it is 0, 
which means that a transaction will be confirmed right after it is processed.

> In order to get easily ABI definition you can open [REMIX editor](https://remix.ethereum.org/) and copy here the smart contract.
Press button **start to compile** and after that you can open **details**. Here you can get and copy the ABI definition.  

## Javascript API

### JSON RPC API

The endpoint provides access to the whole [JSON RPC API](https://github.com/ethereum/wiki/wiki/JSON-RPC)
so make sure you take a look at it.

Basically methods are availables under the same name, only changing the underscore by a dot.
For example if you need to call the method `eth_getTransactionReceipt`, you can call il like
this:

```js
var receipt = app.endpoints.ethereum.eth.transactionReceipt(txHash);
```

Basically parameters are sent directly to the API as indicated in the docs.

These are the methods available:

- `web3_clientVersion`
- `web3_sha3`
- `net_version`
- `net_peerCount`
- `net_listening`
- `eth_protocolVersion`
- `eth_syncing`
- `eth_gasPrice`
- `eth_blockNumber`
- `eth_getBalance`
- `eth_getStorageAt`
- `eth_getTransactionCount`
- `eth_getBlockTransactionCountByHash`
- `eth_getBlockTransactionCountByNumber`
- `eth_getUncleCountByBlockHash`
- `eth_getUncleCountByBlockNumber`
- `eth_getCode`
- `eth_sendRawTransaction`
- `eth_call`
- `eth_estimateGas`
- `eth_getBlockByHash`
- `eth_getBlockByNumber`
- `eth_getTransactionByHash`
- `eth_getTransactionByBlockHashAndIndex`
- `eth_getTransactionByBlockNumberAndIndex`
- `eth_getTransactionReceipt`
- `eth_getUncleByBlockHashAndIndex`
- `eth_getUncleByBlockNumberAndIndex`
- `eth_getWork`

### Send transaction

```js
var res = app.endpoints.ethereum.sendTransaction(aliasOrAddress, fnName, params, fromAddress, signMethod, options);
```

Where:

- `aliasOrAddress`: this is the alias of the address of the contract.
- `fnName`: the name of the function in the contract that will be called.
- `params`: an array with the parameters of the function.
- `fromAddress`: this is the Ethereum address making the call. It needs to match a valid address
  configured in your `signMethod`.
- `signMethod`: the method to sign the transaction. Options are `metamask` or `managed`.
- `options`: here you can specify some options that are only needed if the function is a transaction 
  (in oposition to simple calls that do not change   the state of the blockchain). These are the 
  options:
  - `gasPrice`: this is the gas price to process the transaction. Encoded in hexadecimal 
    (for instance `0xE`). Optional.
  - `gas`: this is the maximum gas to use to process the transaction. Encoded in hexadecimal 
    (for instance `0x92AB0F`). Optional.
  - `value`: the amount of Ether to send in . Encoded in hexadecimal (for instance `0x92AB0F`). Optional.
  - `submitted`: this is the callback that will be called once the transaction is approved and signed 
    (for example, the user approves it using MetaMask) and the transaction is submitted to the
    network for processing. You will get two parameters:
    - `msg`: this is the original message. This is useful to check data of the call or you can
      get data you put in your options (for example a record ID).
    - `res`: this is the response from the approval and submission of the callback. Here you will
      find the transaction hash (`txHash` field) so you can check its status.
  - `confirmed`: this is the callback that will be called once the transaction is confirmed by
     the network. You will get the following parameters:
    - `msg`: this is the original message. This is useful to check data of the call or you can
      get data you put in your options (for example a record ID).
    - `res`: this is the response from the approval and submission of the callback. Here you will
      find the transaction hash so you can check its status.
     - `receipt`: this is the receipt of the transaction. Here you can find information about the
       processing of the transaction. You can check the docs for [eth_getTransactionReceipt](https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_gettransactionreceipt)
       for more details.
     - `events`: this is a list of events that were triggered during the transaction. The format of
       each event is the same as in [Contract events](#contract-event).
  - `error`: if there is an error during the process, this callback will be called. It will get
    the following parameters:
    - `msg`: this is the original message. This is useful to check data of the call or you can
      get data you put in your options (for example a record ID).
    - `res`: here you will find the `errorCode` and `errorMessage` fields indicating the problem. The
      field `errorCode` contains one of these values:
        - `invalidNetwork`: the network the app is trying to use does not match the network the user
          wants to send the transaction to.
        - `invalidAccount`: the account the app is trying to use is not configured for the user.
        - `gasEstimationFail`: the automatic estimation of gas to use failed.
        - `txDeclined`: the user didn't sign the transaction.
        - `txFail`: there was a problem when the transaction was processed by the network. 
        - `txTimeout`: no confirmation from the network after 3 minutes.
     - `receipt`: this is the receipt of the transaction. Here you can find information about the
       processing of the transaction. You can check the docs for [eth_getTransactionReceipt](https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_gettransactionreceipt)
       for more details. This is only present if the error is `txFail`.

This method sends a transaction to the Ethereum network. You can put callbacks in the `options` parameter
to listen for the events to know when the transaction has been confirmed or if there is an error.

Sample:

```js
app.endpoints.ethereum.sendTransaction(
  'greeter', 
  'setGreeting', 
  [action.field('greeting').val()], 
  '0x590782dc744cb95662192bde0da32acf5e99d851',
  'metamask',
  {
    submitted: function(msg, res) {
      sys.logs.info('*** tx submitted');
      sys.logs.info('*** msg: '+JSON.stringify(msg));
      sys.logs.info('*** res: '+JSON.stringify(res));
    },
    confirmed: function(msg, res, receipt, events) {
      sys.logs.info('*** tx confirmed');
      sys.logs.info('*** msg: '+JSON.stringify(msg));
      sys.logs.info('*** res: '+JSON.stringify(res));
      sys.logs.info('*** receipt: '+JSON.stringify(receipt));
      sys.logs.info('*** events: '+JSON.stringify(events));
    },
    error: function(msg, res, receipt) {
      sys.logs.info('*** tx error');
      sys.logs.info('*** msg: '+JSON.stringify(msg));
      sys.logs.info('*** res: '+JSON.stringify(res));
      if (receipt) {
        sys.logs.info('*** receipt: '+JSON.stringify(receipt));
      }
    }
  }
);
```

### Send ether

```js
var res = app.endpoints.ethereum.sendEther(aliasOrAddress, amount, fromAddress, signMethod, options);
```

Very similar to how you can send a transaction to call a function in a contract, but in this case we need to send
the amount (in hexadecimal) instead of the name of the function and the params. The rest is the same.

Same:

```js
app.endpoints.ethereum.sendEther(
  '0xb60e8dd61c5d32be8058bb8eb970870f07233155', 
  '0x9184e72a', 
  '0x590782dc744cb95662192bde0da32acf5e99d851',
  'metamask',
  {
    submitted: function(msg, res) {
      sys.logs.info('*** tx submitted');
      sys.logs.info('*** msg: '+JSON.stringify(msg));
      sys.logs.info('*** res: '+JSON.stringify(res));
    },
    confirmed: function(msg, res, receipt, events) {
      sys.logs.info('*** tx confirmed');
      sys.logs.info('*** msg: '+JSON.stringify(msg));
      sys.logs.info('*** res: '+JSON.stringify(res));
      sys.logs.info('*** receipt: '+JSON.stringify(receipt));
      sys.logs.info('*** events: '+JSON.stringify(events));
    },
    error: function(msg, res, receipt) {
      sys.logs.info('*** tx error');
      sys.logs.info('*** msg: '+JSON.stringify(msg));
      sys.logs.info('*** res: '+JSON.stringify(res));
      if (receipt) {
        sys.logs.info('*** receipt: '+JSON.stringify(receipt));
      }
    }
  }
);
```

### Call function

```js
var res = app.endpoints.ethereum.callFunction(aliasOrAddress, fnName, params, fromAddress);
```

Where:

- `aliasOrAddress`: this is the alias or the address of the contract.
- `fnName`: the name of the function in the contract that will be called.
- `params`: an array with the parameters of the function.
- `fromAddress`: this is the Ethereum address making the call. It is optional.

This method calls a view function in an Ethereum contract (this is a function that does not change
the status of the blockchain).
 
It returns an array with the response of the function. 

Sample:

```js
var res = app.endpoints.ethereum.callFunction('greeter', 'getGreeting', []);
log('greeting: '+JSON.stringify(res));
```


### Compile Solidity code

```js
var res = app.endpoints.ethereum.compileSolidity(code, contractName);
```

Where:

- `code`: is the Solidity code to compile.
- `contractName`: if the source code has multiple contracts but you only want one of them, you
  can specify it here and only that contract will be returned. Optional.

This methods compiles and returns the compiled code and ABI for each contract in the source code.
If you specify `contractName`, instead of an map with all contracts only that specific contract will be
return.

The response looks like this:

```json
{
    "contract123.sol:contract1": {
        "bin": "0x....",
        "abi": "..."
    },
    "contract456.sol:contract2": {
        "bin": "0x....",
        "abi": "..."
    }
}
```

Sample:

```js
var contract = app.endpoints.ethereum.compileSolidity(action.field('code').val(), 'Greeter');
```

### Create contract

```js
app.endpoints.ethereum.createContract(alias, compiledCode, abi, fromAddress, signMethod, options);
```

Where:

- `alias`: the alias of the contract to create. Should be unique or null. If send `null` contract 
address should be required in following transactions.
- `compileCode`: the compiled code of the contract. This could be the `bin` field returned by 
  `compileSolidity`.
- `abi`: the ABI of the contract. This could be the `abi` field returned by `compileSolidity`.
- `fromAddress`: this is the Ethereum address deploying the contract. It needs to match a valid 
  address configured in your `signMethod`.
- `signMethod`: the method to sign the transaction. Available options are `metamask` and `managed`.
- `options`: these are the same options as for `callFunction()`.

This method will deploy the contract into the Ethereum network and register it in the app with
the address assigned by the network. You will be able to get the details of the deployed contract
by using the method `getContract()` (see below).

When a contract is registered, the endpoint will automatically start to listening to events from
that contract.

Sample:

```js
var contract = app.endpoints.ethereum.compileSolidity(action.field('code').val(), 'Greeter');
app.endpoints.ethereum.createContract(
  action.field('alias').val(), 
  contract.bin, 
  contract.abi, 
  '0x590782dc744cb95662192bde0da32acf5e99d851',
  'metamask',
  {
    submitted: function(msg, res) {
      sys.logs.info('*** tx submitted');
      sys.logs.info('*** msg: '+JSON.stringify(msg));
      sys.logs.info('*** res: '+JSON.stringify(res));
    },
    confirmed: function(msg, res, receipt) {
      sys.logs.info('*** tx confirmed');
      sys.logs.info('*** msg: '+JSON.stringify(msg));
      sys.logs.info('*** res: '+JSON.stringify(res));
      sys.logs.info('*** receipt: '+JSON.stringify(receipt));
      sys.logs.info('*** contract address: '+receipt.contractAddress);
    },
    error: function(msg, res, receipt) {
      sys.logs.info('*** tx error');
      sys.logs.info('*** msg: '+JSON.stringify(msg));
      sys.logs.info('*** res: '+JSON.stringify(res));
      if (receipt) {
        sys.logs.info('*** receipt: '+JSON.stringify(receipt));
      }
    }
  }
);
```

### Register contract

``` js
endpoint.registerContract(alias, contractAddress, abi);
```

Registers an existing contract in the endpoint. If there is already a contract registered on the
same address, an error will be thrown.

The `alias` parameter is optional.

### Get contract

``` js
var contract = endpoint.getContract(aliasOrAddress);
```

Returns the contract information by alias or contract address. The format of the contract is like this:

```json
{
    "alias": "aliasOrAddress",
    "address": "0x...",
    "abi": "..."
}
```

### Remove contract

``` js
var contract = endpoint.removeContract(aliasOrAddress);
```

Remove a contract from the app by alias or contract address. Keep in mind that this does not delete the contract in
the Ethereum network (that's not possible), but just unregisters the contract from the app. This
means you won't be able to call any function in the contract and no more events will be received
for this contract.

### Create account

``` js
var address = endpoint.createAccount();
```

Creates a new private key and returns the address associated to that private key.

This accounts are managed by the endpoint so users don't need to rely on external software
like Metamask.

### Import account

``` js
var address = endpoint.importAccount(privateKey);
```

Imports a private key and returns the address associated to it.

This accounts are managed by the endpoint so users don't need to rely on external software
like Metamask.

### Export account

``` js
var account = endpoint.exportAccount(address);
```

{% include warning.html content="Once you export the private key it won't be secure. Only use this for dev purposes." %}

Returns the information of an account, with is the address and private key:

```
{
  "address": "0x...",
  "privateKey": "0x..."
}
```

### Checksum address

``` js
var addressChecksum = endpoint.toChecksumAddress(address);
```

Calculates checksum address according to [Ethereum specifications](https://github.com/ethereum/EIPs/blob/master/EIPS/eip-55.md).

## Events

### Contract event

The event `contractEvent` is sent when a contract registered in the app (either in the endpoint's
configuration or by using `createContract()` or `registerContract()`) triggers an event.

The event will have the following structure:

```json
{
  "type": "ENDPOINT",
  "endpointEvent": "contractEvent",
  "date": 1524678136750,
  "endpoint": "ethereum",
  "data": {
    "rawEvent": {
      "address": "0x0a7a177321f3b3b6e2299e621eb32e892b141b4b",
      "topics": [
        "0x0d332ed5c7d6f1999116748c0eb99c740f276d879d025a5be6435fcf177785de"
      ],
      "data": "0x0000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000568616c6c6f000000000000000000000000000000000000000000000000000000",
      "blockNumber": "0x2f6d30",
      "transactionHash": "0x4aaf274a399ee315dc351b30b2e6ff9525996d4700c4843ec0a625512cd21fd1",
      "transactionIndex": "0x4",
      "blockHash": "0xbfad930ed694aae9fc660951ac4a309701a17e75f02087c72c3ea78431c3005e",
      "logIndex": "0x2",
      "removed": false
    },
    "eventName": "NewGreeting",
    "eventData": {
      "greeting": "hallo"
    }
  }
}
```

In `data.rawEvent` you'll see the raw event coming from Ethereum, while in `data.eventData` you can find
parsed data sent by the contract in the event.

{% include callout.html content="Always check the `removed` flag. Events can be removed if there is a chain reorganization. In this case you will get the original event, the same event with the remove flag, if probably later another event (the one corresponding to the valid block)." type="warning" %} 

### New block

This event is sent when there is a new block in the chain. The event contains information about that block, like 
hash and number:

```json
{
  "type": "ENDPOINT",
  "endpointEvent": "newBlock",
  "date": 1524678136750,
  "endpoint": "ethereum",
  "data": {
      "number": "0x1b4",
      "hash": "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d05921026d1527331",
      "parentHash": "0x9646252be9520f6e71339a8df9c55e4d7619deeb018d2a3f2d21fc165dde5eb5",
      "nonce": "0xe04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f2",
      "sha3Uncles": "0x1dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347",
      "logsBloom": "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d05921026d1527331",
      "transactionsRoot": "0x56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421",
      "stateRoot": "0xd5855eb08b3387c0af375e9cdb6acfc05eb8f519e419b874b6ff2ffda7ed1dff",
      "miner": "0x4e65fda2159562a496f9f3522f89122a3088497a",
      "difficulty": "0x027f07",
      "totalDifficulty": "0x027f07",
      "extraData": "0x0000000000000000000000000000000000000000000000000000000000000000",
      "size": "0x027f07",
      "gasLimit": "0x9f759",
      "gasUsed": "0x9f759",
      "timestamp": "0x54e34e8e",
      "uncles": ["0x1606e5...", "0xd5145a9..."]
  }
}
```

### Block removed 

This event is sent when due to a chain reorganization, a block is removed. You will get the same
information as when a there is a new block:

```json
{
  "type": "ENDPOINT",
  "endpointEvent": "blockRemoved",
  "date": 1524678136750,
  "endpoint": "ethereum",
  "data": {
      "number": "0x1b4",
      "hash": "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d05921026d1527331",
      "parentHash": "0x9646252be9520f6e71339a8df9c55e4d7619deeb018d2a3f2d21fc165dde5eb5",
      "nonce": "0xe04d296d2460cfb8472af2c5fd05b5a214109c25688d3704aed5484f9a7792f2",
      "sha3Uncles": "0x1dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347",
      "logsBloom": "0xe670ec64341771606e55d6b4ca35a1a6b75ee3d5145a99d05921026d1527331",
      "transactionsRoot": "0x56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421",
      "stateRoot": "0xd5855eb08b3387c0af375e9cdb6acfc05eb8f519e419b874b6ff2ffda7ed1dff",
      "miner": "0x4e65fda2159562a496f9f3522f89122a3088497a",
      "difficulty": "0x027f07",
      "totalDifficulty": "0x027f07",
      "extraData": "0x0000000000000000000000000000000000000000000000000000000000000000",
      "size": "0x027f07",
      "gasLimit": "0x9f759",
      "gasUsed": "0x9f759",
      "timestamp": "0x54e34e8e",
      "uncles": ["0x1606e5...", "0xd5145a9..."]
  }
}
``` 

### Transaction removed

This event is sent when a transaction that was processed through the endpoint (by using `sendTransaction()`)
is removed due to a chain reorganization.

Keep in mind that you won't get this event for transactions executed outside the endpoint (another Dapp calling 
the contract's function).

The data of the event is the receipt of the transaction that was removed:

```json
{
  "type": "ENDPOINT",
  "endpointEvent": "blockRemoved",
  "date": 1524678136750,
  "endpoint": "ethereum",
  "data": {
     "transactionHash": "0xb903239f8543d04b5dc1ba6579132b143087c68db1b2168786408fcbce568238",
     "transactionIndex":  "0x1",
     "blockNumber": "0xb",
     "blockHash": "0xc6ef2fc5426d6ad6fd9e2a26abeab0aa2411b7ab17f30a99d3cb96aed1d1055b",
     "cumulativeGasUsed": "0x33bc",
     "gasUsed": "0x4dc",
     "contractAddress": "0xb60e8dd61c5d32be8058bb8eb970870f07233155",
     "logs": [],
     "logsBloom": "0x00...0",
     "status": "0x1"
  }
}
```

## About SLINGR

SLINGR is a low-code rapid application development platform that accelerates development, with robust architecture for integrations and executing custom workflows and automation.

[More info about SLINGR](https://slingr.io)

## License

This endpoint is licensed under the Apache License 2.0. See the `LICENSE` file for more details.

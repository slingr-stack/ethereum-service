pragma solidity ^0.4.21;

contract TestContract {

    string myString = "First";
    bool myBool = false;
    int myInt = 564;
    uint8 myUint8 = 32;
    address myAddress = 0xca35b7d915458ef540ade6068dfe2f44e8fa733c;
    uint[2] uintArr = [23, 42];
    int[] intArr;
    address[] addresses;
    bytes myBytes;

    event MyEvent(
        string _name,
        uint _value
    );

    event MySecondEvent(
        string _name,
        uint _value,
        address indexed _myAddress
    );

    event Transfer(address indexed from, address indexed to, uint256 value);

    function setString(string _myString) public {
        myString = _myString;
    }

    function setStringAndEvent(string _myString) public {
        myString = _myString;
        emit MyEvent(_myString, 101);
    }

    function setStringAndTwoEvents(string _myString) public {
        myString = _myString;
        emit Transfer(msg.sender, myAddress, 408);
    }

    function getString() public constant returns (string) {
        return myString;
    }

    function setBytes(bytes _myBytes) public {
        myBytes = _myBytes;
    }

    function getBytes() public constant returns (bytes) {
        return myBytes;
    }

    function setBool(bool _myBool) public {
        myBool = _myBool;
    }

    function getBool() public constant returns (bool) {
        return myBool;
    }

    function setInt(int _myInt) public {
        myInt = _myInt;
    }

    function getInt() public constant returns (int) {
        return myInt;
    }

    function setUint8(uint8 _myUint8) public {
        myUint8 = _myUint8;
    }

    function getUint8() public constant returns (uint8) {
        return myUint8;
    }

    function setAddress(address _myAddress) public {
        myAddress = _myAddress;
    }

    function getAddress() public constant returns (address) {
        return myAddress;
    }

    function setArrayUint(uint[2] _arr) public {
        uintArr = _arr;
    }

    function getUintArr() public constant returns (uint[2]) {
        return uintArr;
    }

    function setIntArr(int a, int b, int c) public {
        intArr.push(a);
        intArr.push(b);
        intArr.push(c);
    }

    function getIntArr() public constant returns(int[]) {
        return intArr;
    }

    function result1() public constant returns(string, address, int) {
        return (myString, myAddress, myInt);
    }

    function result2() public constant returns(string, bool, int[]) {
        return (myString, myBool, intArr);
    }

    function setAddresses() public {
        addresses.push(0xca35b7d915458ef540ade6068dfe2f44e8fa733c);
        addresses.push(0xca35b7d915458ef540ade6068dfe2f44e8fa7311);
        addresses.push(0xca35b7d915458ef540ade6068dfe2f44e8fa7333);
    }

    function result3() public constant returns(string, bool, address[]) {
        return (myString, true, addresses);
    }
}

package be.limero.network;

import be.limero.util.Bytes;
import be.limero.util.Cbor;
import be.limero.util.Slip;

public class Request {
	static public enum Cmd {
		PING, EXEC, RESET, MODE_BOOTLOADER, MODE_USER,STM32_OUTPUT,LOG_OUTPUT
	};

	public Cmd _cmd;
	public int _id = 1;
	public Bytes _data;

	static int messageId = 1;

	int nextId() {
		return messageId++ & 0xFFFF;
	}

	Request() {
		_cmd = Cmd.PING;
		_id = nextId();
		_data = new Bytes(256);
	}

	public Request(Cmd cmd, byte[] data) {
		_cmd = cmd;
		_id = nextId();
		_data = new Bytes(data);
	}

	// <cmd><id:int><data:bytes>
	public Cbor toCbor() {
		Cbor cbor = new Cbor(512);
		cbor.add(_cmd.ordinal());
		cbor.add(_id);
		cbor.add(_data);
		return cbor;
	}

	public String toString() {
		return String.format(" cmd : %s ,id : %s, data : %s ", _cmd.toString(),
				_id, _data.toHex());
	}
	
	public Bytes toSlip() {
		Bytes bytes=new Bytes(1024);
		bytes = toCbor();
		Slip.addCrc(bytes);
		bytes = Slip.encode(bytes);
		return bytes;
	}

}

package be.limero.programmer.commands;

import java.util.Vector;
import java.util.logging.Logger;

import be.limero.common.Bytes;
import be.limero.common.Cbor;
import be.limero.common.LogHandler;
import be.limero.programmer.Stm32Model;

public class Stm32Msg extends Cbor {
	private static final Logger log = Logger.getLogger(Stm32Msg.class.getName());
	static Integer _nextMessageId = 0;
	static Boolean enabled;
	
	public enum CMD {
		INVALID, STM32_CMD_UART_DATA, STM32_CMD_BOOT_ENTER, STM32_CMD_BOOT_REQ, STM32_CMD_RESET
	};

	static public Integer getNextId() {
		return _nextMessageId++;
	}

	public Stm32Msg(int size) {
		super(size);
		data = new Vector<Bytes>();
		acks = new Vector<Integer>();
		enabled=true;
	}

	public Stm32Msg(byte[] src) {
		super(src);
		data = new Vector<Bytes>();
		acks = new Vector<Integer>();
		enabled=true;
	}
	
	static Bytes addCrc(Bytes bytes) {
		byte xor ;
		bytes.offset(0);
		xor=bytes.read();
		while (bytes.hasData())
			xor ^= bytes.read();
		bytes.write(xor);
		return bytes;
	}
	
	static Bytes addCrc(byte b) {
		Bytes bytes = new Bytes(2);
		byte xor ;
		bytes.write(b);
		bytes.offset(0);
		xor=bytes.read();
		while (bytes.hasData())
			xor ^= bytes.read();
		bytes.write(xor);
		return bytes;
	}

	protected static Bytes complementByte(byte cmd) {
		Bytes bytes = new Bytes(2);
		bytes.write(cmd);
		bytes.write(~cmd);
		return bytes;

	}
	
	protected static Bytes crcBytes(byte cmd) {
		Bytes bytes = new Bytes(2);
		bytes.write(cmd);
		addCrc(bytes);
		return bytes;

	}

	static Bytes crcBytes(byte[] data) {
		Bytes bytes = new Bytes(data.length + 1);
		for (int i = 0; i < data.length; i++)
			bytes.write(data[i]);
		addCrc(bytes);
		return bytes;
	}
	
	Bytes quadToBytes(int address) {
		Bytes bytes=new Bytes(10);
		bytes.write(address>>24);
		bytes.write(address>>16);
		bytes.write(address>>8);
		bytes.write(address);
		return bytes;
	}

	public enum Field {
		INVALID, CMD, MESSAGE_ID, ERROR, DATA, ACKS
	};

	public Integer cmd;
	public Integer messageId;
	public Integer error;
	public Vector<Bytes> data;
	public Vector<Integer> acks;

	public Stm32Msg parse() {
		
		offset(0);
		cmd = getInteger();
		messageId = getInteger();
		error = getInteger();
		acks.clear();
		data.clear();
		while (hasData()) {
			data.add(getBytes());
			if (hasData())
				acks.add(getInteger());
		}
		return this;
	}
	
	public void handle(Stm32Model model) {
		
	}

	public Stm32Msg build() {
		clear();
		add(cmd);
		add(messageId);
		add(error);
		for (int i = 0; i < data.size(); i++) {
			add(data.get(i));
			add(acks.get(i));
		}
		return this;
	}
	
	public String toString(){
		StringBuilder sb=new StringBuilder();
		sb.append("cmd:").append(cmd).append(",messageId:").append(messageId);
		sb.append(",error:").append(error);
		return sb.toString();
	}
	
	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(Boolean enabled) {
		Stm32Msg.enabled = enabled;
	}

	public static void main(String[] args) {
		LogHandler.buildLogger();
		Stm32Msg msg = new Stm32Msg(1000);
		msg.cmd =  Stm32Msg.CMD.STM32_CMD_BOOT_REQ.ordinal();
		msg.build();
		Stm32Msg msg2 = new Stm32Msg(msg.bytes());
		msg2.parse();
		Stm32Msg.log.info(msg.toHex());
		Stm32Msg.log.info(msg2.toString());
		Stm32GetVersionCommands m=new Stm32GetVersionCommands();
		Stm32Msg.log.info(m.toHex());
	}

}

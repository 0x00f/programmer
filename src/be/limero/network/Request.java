package be.limero.network;

import java.util.Arrays;

import be.limero.util.Bytes;
import be.limero.util.Cbor;
import be.limero.util.Slip;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import io.vertx.core.json.JsonObject;

public class Request {

	public String cmd;
	public int id = 1;
	public Bytes data;

	static int messageId = 1;

	int nextId() {
		return messageId++ & 0xFFFF;
	}

	Request() {
		cmd = "";
		id = nextId();
		data = new Bytes(256);
	}

	public Request(String cd, byte[] data) {
		cmd = cd;
		id = nextId();
		this.data = new Bytes(data);
	}

	public Request(String cmd, int id, byte[] data) {
		this.cmd = cmd;
		this.id = id;
		this.data = new Bytes(data);
	}

	// <cmd><id:int><data:bytes>
	public Cbor toCbor() {
		Cbor cbor = new Cbor(512);
		cbor.add(cmd);
		cbor.add(id);
		cbor.add(data);
		return cbor;
	}

	public String toString() {
		return String.format(" cmd : %s ,id : %s, data : %s ", cmd, id, data.toHex());
	}

	public Bytes toSlip() {
		Bytes bytes = new Bytes(1024);
		bytes = toCbor();
		Slip.addCrc(bytes);
		bytes = Slip.encode(bytes);
		return bytes;
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.put("id", id);
/*		ByteBuf base64 = Base64.encode(Unpooled.copiedBuffer(data.bytes()));
		StringBuilder sb=new StringBuilder();
		while( base64.readableBytes()>0) {
			sb.append((char)base64.readByte());
		}*/
		json.put("data", data.bytes());
		json.put("request", cmd);
		return json;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

}

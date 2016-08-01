package be.limero.network;

import java.util.logging.Level;
import java.util.logging.Logger;

import be.limero.util.Bytes;
import be.limero.util.Cbor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import io.vertx.core.json.JsonObject;

public class Reply {
	private static final Logger log = Logger.getLogger(Reply.class.getName());

	String cmd;
	int id;
	int error;
	byte[] data;

	boolean fromJson(String s) {
		try {
			JsonObject json = new JsonObject(s);
			cmd = json.getString("reply");
			id = json.getInteger("id");
			error = json.getInteger("error");
/*			String sData = json.getString("data");
			ByteBuf in = Unpooled.copiedBuffer(sData.getBytes());
			ByteBuf bb = Base64.decode(in);
			data = bb.array();*/
			data =  json.getBinary("data");
		} catch (Exception ex) {
			log.log(Level.SEVERE, " json parsing failed ", ex);
			return false;
		}
		return true;
	}

	boolean parse(Bytes data) {
		try {
			Cbor cbor = new Cbor(1024);
			data.offset(0);
			while (data.hasData()) {
				cbor.write(data.read());
			}
			cbor.offset(0);
			cmd = cbor.getString();
			error = cbor.getInteger();
			this.data = cbor.getBytes().bytes();
		} catch (Exception e) {
			log.log(Level.SEVERE, "parse failes", e);
			return false;
		}
		return true;
	}

	public int getId() {
		return id;
	}

	public int getError() {
		return error;
	}

	/**
	 * @return the cmd
	 */
	public String getCmd() {
		return cmd;
	}

	/**
	 * @return the data
	 */
	public byte[] getData() {
		return data;
	}

}

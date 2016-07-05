package be.limero.network;

import java.util.logging.Level;
import java.util.logging.Logger;
import be.limero.network.Request.Cmd;
import be.limero.util.Bytes;
import be.limero.util.Cbor;

public class Reply {
	private static final Logger log = Logger.getLogger(Reply.class.getName());

	Request.Cmd cmd;
	int id;
	int error;
	byte[] output;

	boolean parse(Bytes data) {
		try {
			Cbor cbor = new Cbor(1024);
			data.offset(0);
			while (data.hasData()) {
				cbor.write(data.read());
			}
			cbor.offset(0);
			cmd = Cmd.values()[cbor.getInteger()];
			error = cbor.getInteger();
			output = cbor.getBytes().bytes();
		} catch (Exception e) {
			log.log(Level.SEVERE, "parse failes", e);
			return false;
		}
		return true;
	}

	public Request.Cmd getCmd() {
		return cmd;
	}

	public int getId() {
		return id;
	}

	public int getError() {
		return error;
	}

	public byte[] getOutput() {
		return output;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}

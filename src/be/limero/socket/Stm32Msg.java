package be.limero.socket;

import java.io.Serializable;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;

import be.limero.common.Bytes;
import be.limero.common.Errno;

import org.json.*;

public class Stm32Msg {
	private static final Logger log = Logger.getLogger(Stm32Msg.class.getName());
	public Integer id;
	public Cmd cmd;
	public byte[] data;
	
	public enum JobStatus implements Serializable{
	    INCOMPLETE,
	    INPROGRESS,
	    ABORTED,
	    COMPLETED;

	    public String getStatus() {
	        return this.name();
	    }
	}

	public enum Cmd {
		PUT, GET;
		public String getStatus() {
	        return this.name();
	    }
	};
	
	public String getCmd(){
		return cmd.toString();
	}

	public Stm32Msg() {
		id = 1234;
		cmd = Cmd.PUT;
		data = new byte[] { 0x1, 0x2,0x3 };
	}

	public String toJson() {
		JSONObject json = new JSONObject();
		json.put("id", id);
		json.put("cmd", cmd.toString());
		String b = Base64.getEncoder().encodeToString(data);
		json.put("data", b);
		return json.toString();
	}

	public int fromJson(String source) {
		try {
			JSONObject json = new JSONObject(source);
			cmd = json.getEnum(Cmd.class, "cmd");
			id = json.getInt("id");
			data = Base64.getDecoder().decode(json.getString("data"));
		} catch (Exception e) {
			log.log(Level.SEVERE, "");
			return Errno.EINVAL;
		}
		return Errno.E_OK;
	}
	
	public String toString(){
		Bytes bytes=new Bytes(data);
		return " id: "+id+" cmd: "+cmd.toString()+" data: "+bytes.toString();
	}

}

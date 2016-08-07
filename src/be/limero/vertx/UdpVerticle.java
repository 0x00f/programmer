package be.limero.vertx;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;

import be.limero.util.Cbor;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class UdpVerticle extends AbstractVerticle {
	private final static Logger log =Logger.getLogger(UdpVerticle.class.toGenericString());
	DatagramSocket socket;
	EventBus eb;
	int remotePort = 1883;
	String remoteHost = "192.168.0.132";
	int localPort = 3881;
	HashMap<Integer, Message<Object>> replies;
	Long lastCommand;
	int lastId;


	/*
	 * (non-Javadoc)
	 * 
	 * @see io.vertx.core.AbstractVerticle#start()
	 */
	@Override
	public void start() throws Exception {
		eb = vertx.eventBus();
		replies = new HashMap<Integer, Message<Object>>();
		super.start();
		socket = vertx.createDatagramSocket(new DatagramSocketOptions());
		socket.listen(localPort, "0.0.0.0", asyncResult -> {
			if (asyncResult.succeeded()) {
				socket.handler(packet -> {
					String msg = new String(packet.data().getBytes(), StandardCharsets.UTF_8);
					// log.info("UDP RXD :"+msg);
					try {
						JsonObject json = new JsonObject(msg);
						onUdpMessage(json);
					} catch (Exception e) {
						System.out.println(msg);
						// log.fatal(" JSON parsing failed "+ e.getMessage());
					}

				});
			} else {
				log.info("Listen failed" + asyncResult.cause());
			}
		});

		eb.consumer("proxy", message -> {
			if (message.body() instanceof JsonObject) {
				JsonObject json = (JsonObject) message.body();
				if (json.getString("request") == ("connect")) {
					remoteHost = json.getString("host");
					remotePort = json.getInteger("port");
					message.reply(new JsonObject().put("reply", "connect").put("connected", true).put("error", 0));
				} else if (json.getString("request") == ("disconnect")) {
					message.reply(new JsonObject().put("reply", "disconnect").put("connected", false).put("error", 0));
				} else {
					try {
						replies.put(json.getInteger("id"),(Message<Object>) message);
						if (canSendNext()) sendNext();						
					} catch (Exception e) {
						log.log(Level.SEVERE, " udp send failed ", e);
					}
					
				}
			}
			log.info("EB : " + message.body());
		});

	}
	
	boolean canSendNext() {
		if (lastCommand == null) {
			lastCommand = System.currentTimeMillis();
			return true;
		} else if (lastCommand < (System.currentTimeMillis() - 10)) {
			lastCommand = System.currentTimeMillis();
			return true;
		}
		return false;
	}
	
	void sendNext(){
		try {
			if ( replies.isEmpty() ) {
				lastCommand=null;
				return;
			}
			lastCommand = System.currentTimeMillis();
			Iterator <Message<Object>> it=replies.values().iterator();
			Message<Object> first = it.next();
			JsonObject json=(JsonObject)first.body();
			socket.send(json.encode(), remotePort, remoteHost, returnData -> {
			});
		} catch (Exception e) {
			log.log(Level.SEVERE, " send failed.",e);
		}
	}

	void connect(String host, int port) {

	}

	void onEventBusMessage(JsonObject msg) {
		log.info(" EventBus : " + msg);
	}

	void onUdpMessage(JsonObject json) {
		log.info("UDP : " + json);
		replies.get(json.getInteger("id")).reply(json);
		replies.remove(json.getInteger("id"));
		if ( json.getInteger("error") !=0 ) {
			log.warning(" error occured , cancelling queue ");
			replies.forEach((k,message)->{
				replies.get(k).fail(-1, "previous command failed");										
			});
			replies.clear();
		}
		sendNext();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.vertx.core.AbstractVerticle#stop()
	 */
	@Override
	public void stop() throws Exception {
		// TODO Auto-generated method stub
		super.stop();
	}

	static int nextId=0;
	
	static long newId() {
		return nextId++;
	}

	public static void main(String[] args) {
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
		UdpVerticle udp = new UdpVerticle();
		Vertx.vertx().deployVerticle(udp);
		try {
			Thread.sleep(1000);
		} catch (Exception e) {
		}

		EventBus eb = udp.getVertx().eventBus();
		int interval = 1000;
		while (true) {
			try {
				Thread.sleep(interval);
				eb.send("proxy", new JsonObject().put("request", "status").put("id", newId()));
				eb.send("proxy", new JsonObject().put("request", "reset").put("id", newId()));
				eb.send("proxy", new JsonObject().put("request", "getId").put("id", newId()));
				eb.send("proxy", new JsonObject().put("request", "get").put("id", newId()));
				eb.send("proxy", new JsonObject().put("request", "readMemory").put("address", 0x08000000)
						.put("length", 256).put("id", newId()));
				eb.send("proxy", new JsonObject().put("request", "writeMemory").put("address", 0x08000000)
						.put("data", new byte[] { 1, 2, 3, 4 }).put("id", newId()));
				eb.send("proxy", new JsonObject().put("request", "readMemory").put("address", 0x08000000)
						.put("length", 256).put("id", newId()));
				eb.send("proxy", new JsonObject().put("request", "eraseMemory")
						.put("pages", new byte[] { 0, 1, 2, 3, 4 }).put("id", newId()));
				eb.send("proxy", new JsonObject().put("request", "readMemory").put("address", 0x08000000)
						.put("length", 256).put("id", newId()));
				eb.send("proxy", new JsonObject().put("request", "writeMemory").put("address", 0x08000000)
						.put("data", new byte[] { 1, 2, 3, 4 }).put("id", newId()));
				eb.send("proxy", new JsonObject().put("request", "eraseAll").put("id", newId()));
				eb.send("proxy", new JsonObject().put("request", "readMemory").put("address", 0x08000000)
						.put("length", 256).put("id", newId()));
				eb.send("proxy", new JsonObject().put("request", "go").put("address", 0x08000000).put("id", newId()));
				Thread.sleep(20000);
			} catch (Exception e) {
			}
		}
	}

}

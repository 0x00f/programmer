package be.limero.vertx;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class UdpVerticle extends AbstractVerticle {
	private final static Logger log = Logger.getLogger(UdpVerticle.class.toGenericString());
	DatagramSocket socket;
	EventBus eb;
	int remotePort = 1883;
	String remoteHost = "192.168.0.132";
	int localPort = 3881;
	// HashMap<Integer, Message<Object>> replies;
	ArrayBlockingQueue<Message<Object>> queue;
	Long lastCommand;
	int lastId;
	long udpResponseTimer;
	int retryCount = 0;
	JsonObject outstandingUdp = null;
	boolean udpReplyPending = false;

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.vertx.core.AbstractVerticle#start()
	 */
	@Override
	public void start() throws Exception {
		eb = vertx.eventBus();
		// replies = new HashMap<Integer, Message<Object>>();
		queue = new ArrayBlockingQueue<Message<Object>>(2048);
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
						log.fine(" JSON parsing failed " + e.getMessage());
					}

				});
			} else {
				log.info("Listen failed" + asyncResult.cause());
			}
		});

		eb.consumer("proxy", message -> {
			if (message.body() instanceof JsonObject) {
				JsonObject json = (JsonObject) message.body();
				if (json.getString("request").equals("connect")) {
					log.info(" UDP connect ");
					remoteHost = json.getString("host");
					remotePort = json.getInteger("port");
					message.reply(new JsonObject().put("reply", "connect").put("connected", true).put("error", 0));
				} else if (json.getString("request").equals("disconnect")) {
					message.reply(new JsonObject().put("reply", "disconnect").put("connected", false).put("error", 0));
					log.info(" UDP disconnect ");
				} else {
					try {
						queue.add(message);
						sendNext();
					} catch (Exception e) {
						log.log(Level.SEVERE, " udp send failed ", e);
					}

				}
			}
			// log.info("EB : " + message.body());
		});

	}

	/*
	 * on UDP received , check match if ok : remove from queue & reply & send
	 * Next if nok : ignore on timeout : if pending() : resend on cmd received :
	 * if (!pending) send();
	 * 
	 * on timeout : if ( pending ) send()
	 * 
	 */

	void sendUdp(JsonObject json) {
		try {
			log.info(" UDP TXD :" + json);
			udpResponseTimer = vertx.setTimer(2000, id -> {
				if (retryCount < 4) {
					log.info(" TIMEOUT : retrying !");
					udpReplyPending = false;
					outstandingUdp = null;
					sendNext();
				} else {
					log.info(" to many retries,cancelling all requests ! ");
					queue.clear();
					retryCount = 0;
				}
			});
			socket.send(json.encode(), remotePort, remoteHost, returnData -> {
			});
			udpReplyPending = true;
			outstandingUdp = json;
		} catch (Exception e) {
			log.log(Level.SEVERE, " send failed.", e);
		}
	}

	void sendNext() {
		try {
			if (queue.isEmpty()) {
				outstandingUdp = null;
				udpReplyPending = false;
				return;
			}
			if (!udpReplyPending) {
				Message<Object> message = queue.peek();
				JsonObject json = (JsonObject) message.body();
				sendUdp(json);
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, " send failed.", e);
		}
	}

	void connect(String host, int port) {

	}

	void onEventBusMessage(JsonObject msg) {
		log.info(" EventBus : " + msg);
	}

	void onUdpMessage(JsonObject json) {
		try {
			log.info("UDP RXD : " + json);
			if (queue.isEmpty()) {
				log.info("------------------ UDP received after all reply ,ignored double ");
				return;
			}
			if (outstandingUdp.getInteger("id").equals(json.getInteger("id"))) {
				queue.take().reply(json);
				retryCount = 0;
				outstandingUdp = null;
				udpReplyPending = false;
				vertx.cancelTimer(udpResponseTimer);
			} else {
				log.info("-------------- UDP received no matching in queue, pending : "
						+ outstandingUdp.getInteger("id") + " received : " + json.getInteger("id"));
			}
			if (json.getInteger("error") != 0) {
				log.warning(" error occured , cancelling queue ");
				while (!queue.isEmpty()) {
					Message<Object> msg = queue.take();
					msg.fail(-1, "previous command failed");
				}
			}
			sendNext();
		} catch (Exception e) {
			log.log(Level.SEVERE, " udp message handling fails ", e);
		}
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
		int nextId = 0;
		while (true) {
			try {
				Thread.sleep(interval);
				eb.send("proxy", new JsonObject().put("request", "status").put("id", nextId++));
				eb.send("proxy", new JsonObject().put("request", "reset").put("id", nextId++));
				eb.send("proxy", new JsonObject().put("request", "getId").put("id", nextId++));
				eb.send("proxy", new JsonObject().put("request", "get").put("id", nextId++));
				eb.send("proxy", new JsonObject().put("request", "readMemory").put("address", 0x08000000)
						.put("length", 256).put("id", nextId++));
				eb.send("proxy", new JsonObject().put("request", "writeMemory").put("address", 0x08000000)
						.put("data", new byte[] { 1, 2, 3, 4 }).put("id", nextId++));
				eb.send("proxy", new JsonObject().put("request", "readMemory").put("address", 0x08000000)
						.put("length", 256).put("id", nextId++));
				eb.send("proxy", new JsonObject().put("request", "eraseMemory")
						.put("pages", new byte[] { 0, 1, 2, 3, 4 }).put("id", nextId++));
				eb.send("proxy", new JsonObject().put("request", "readMemory").put("address", 0x08000000)
						.put("length", 256).put("id", nextId++));
				eb.send("proxy", new JsonObject().put("request", "writeMemory").put("address", 0x08000000)
						.put("data", new byte[] { 1, 2, 3, 4 }).put("id", nextId++));
				eb.send("proxy", new JsonObject().put("request", "eraseAll").put("id", nextId++));
				eb.send("proxy", new JsonObject().put("request", "readMemory").put("address", 0x08000000)
						.put("length", 256).put("id", nextId++));
				eb.send("proxy", new JsonObject().put("request", "go").put("address", 0x08000000).put("id", nextId++));
				Thread.sleep(20000);
			} catch (Exception e) {
			}
		}
	}

}

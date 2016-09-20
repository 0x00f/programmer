package be.limero.vertx;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttToken;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.datagram.DatagramPacket;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class MqttVerticle2 extends AbstractVerticle implements IMqttMessageListener {
	private final static Logger log = Logger.getLogger(MqttVerticle2.class.toGenericString());
	MqttAsyncClient mqtt;
	MqttConnectOptions mqttConnectOptions;
	EventBus eb;
	int remotePort = 1883;
	String remoteHost = "test.mosquitto.org";
	int localPort = 3881;
	// HashMap<Integer, Message<Object>> replies;
	ArrayBlockingQueue<Message<Object>> queue;
	Long lastCommand;
	int lastId;
	long mqttResponseTimer;
	int retryCount = 0;
	JsonObject outstandingMqtt = null;
	boolean mqttReplyPending = false;

	void mqttListen() {
		try {
			mqtt.subscribe("wibo1/#", 1, this);
		} catch (MqttException e) {
			log.log(Level.SEVERE, " mqtt subscribe failed ", e);
		}
	}

	void mqttUnlisten() {
		try {
			mqtt.unsubscribe("wibo1/#");
		} catch (MqttException e) {
			log.log(Level.SEVERE, " mqtt unsubscribe failed ", e);
		}
	}

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

		mqttConnectOptions = new MqttConnectOptions();
		mqttConnectOptions.setCleanSession(true);
		mqttConnectOptions.setWill("programmer/alive", "true".getBytes(), 2, false);
		mqttConnectOptions.setKeepAliveInterval(20);

		eb.consumer("proxy", message -> {
			if (message.body() instanceof JsonObject) {
				JsonObject json = (JsonObject) message.body();
				if (json.getString("request").equals("connect")) {
					try {
						remoteHost = json.getString("host");
						remotePort = json.getInteger("port");
						mqtt = new MqttAsyncClient("tcp://" + remoteHost, "stm32-programmer");
						IMqttToken token = mqtt.connect();
						token.waitForCompletion(5000);
						if (token.isComplete()) {
							log.info(" MQTT connected tcp://" + remoteHost + ":" + remotePort);
							mqttListen();
						} else
							log.info(" MQTT connection failed  tcp://" + remoteHost + ":" + remotePort);

						message.reply(new JsonObject().put("reply", "connect").put("connected", true).put("error", 0));
					} catch (Exception e) {

					}
				} else if (json.getString("request").equals("disconnect")) {
					message.reply(new JsonObject().put("reply", "disconnect").put("connected", false).put("error", 0));
					mqttUnlisten();
					log.info(" MQTT disconnect ");
				} else {
					try {
						log.info(" MQTT QUEUE :" + message);
						queue.add(message);
						sendNext();
					} catch (Exception e) {
						log.log(Level.SEVERE, " mqtt send failed ", e);
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

	void sendMqtt(JsonObject json) {
		try {
			mqttResponseTimer = vertx.setTimer(2000, id -> {

				if (retryCount < 4) {
					log.info(" TIMEOUT : retrying !");
					mqttReplyPending = false;
					outstandingMqtt = null;
					retryCount++;
					sendNext();
				} else {
					log.info(" to many retries,cancelling all requests ! ");
					queue.clear();
					retryCount = 0;
				}
			});
			log.info(" MQTT PUBLISH :" +"put/wibo1/bootloader/request = "+ json);

			mqtt.publish("put/wibo1/bootloader/request", json.toString().getBytes(), 0, false, null,
					new IMqttActionListener() {

						@Override
						public void onFailure(IMqttToken toke, Throwable exc) {
							log.log(Level.SEVERE, " publish failed ", exc);
						}

						@Override
						public void onSuccess(IMqttToken token) {
							// log.info(" publish succeeded " +
							// token);
						}

					});
			mqttReplyPending = true;
			outstandingMqtt = json;
		} catch (Exception e) {
			log.log(Level.SEVERE, " send failed.", e);
		}
	}

	void sendNext() {
		try {
			if (queue.isEmpty()) {
				outstandingMqtt = null;
				mqttReplyPending = false;
				return;
			}
			if (!mqttReplyPending) {
				Message<Object> message = queue.peek();
				JsonObject json = (JsonObject) message.body();
				sendMqtt(json);
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, " send failed.", e);
		}
	}

	void connect(String host, int port) {

	}
/*
	void onEventBusMessage(JsonObject msg) {
		log.info(" EventBus : " + msg);
	}
*/
	void onMqttMessage(String topic, String msg) {
		log.info("MQTT RXD :" + msg);
		try {
			JsonObject json = new JsonObject(msg);
			try {
				if (queue.isEmpty()) {
					log.info("------------------ MQTT received after all reply ,ignored double ");
					return;
				}
				if (outstandingMqtt.getInteger("id").equals(json.getInteger("id"))) {
					queue.take().reply(json); // send reply to controller
					// log.info(" reply send");
					retryCount = 0;
					outstandingMqtt = null;
					mqttReplyPending = false;
					vertx.cancelTimer(mqttResponseTimer);
				} else {
					log.info("-------------- MQTT received no matching in queue, pending : "
							+ outstandingMqtt.getInteger("id") + " received : " + json.getInteger("id"));
				}
				if (json.getInteger("error") != 0) {
					log.warning(" error occured , cancelling queue ");
					while (!queue.isEmpty()) {
						Message<Object> m = queue.take();
						m.fail(-1, "previous command failed");
					}
				}
				sendNext();
			} catch (Exception e) {
				log.log(Level.SEVERE, " udp message handling fails ", e);
			}
		} catch (Exception e) {
			// System.out.println(msg);
			// none JSON
			eb.send("controller", new JsonObject().put("from", "UDP").put("request", "log").put("data", msg));
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

	public static void main_old(String[] args) {
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
		MqttVerticle2 udp = new MqttVerticle2();
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

	@Override
	public void messageArrived(String topic, MqttMessage msg) throws Exception {
		String message = new String(msg.getPayload(), StandardCharsets.UTF_8);
		if (topic.endsWith("system/log")) {
			eb.send("controller", new JsonObject().put("from", "UDP").put("request", "log").put("data", message));
		} else if (topic.endsWith("reply")) {
			onMqttMessage(topic, message);
		} else if (topic.endsWith("usart/rxd")) {
			eb.send("controller",
					new JsonObject().put("event", "data").put("topic", topic).put("message", message));
			// log.info(" PUBLISH " + topic + " = " + message);
		} else {
			eb.send("controller",
					new JsonObject().put("reply", "settings").put("topic", topic).put("message", message));
			// log.info(" PUBLISH " + topic + " = " + message);
		}

	}

}

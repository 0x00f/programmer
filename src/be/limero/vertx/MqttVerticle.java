package be.limero.vertx;

import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.Listener;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

import be.limero.network.RequestQueue;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class MqttVerticle extends AbstractVerticle {
	private final static Logger log = Logger.getLogger(MqttVerticle.class.toString());
	MQTT mqtt = new MQTT();
	CallbackConnection connection;
//	static Vertx vertx = Vertx.vertx();
	private  static EventBus eb ;
	RequestQueue queue = new RequestQueue(300);
	boolean mqttConnected;

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.vertx.core.AbstractVerticle#start()
	 */
	@Override
	public void start() throws Exception {

		super.start();

		log.info("Mqtt verticle started ");
		eb = getVertx().eventBus();
		eb.consumer("proxy", msg -> {
			log.info(" received EB message :" + msg);
			onEventBusMessage(msg);
		});

		mqtt = new MQTT();
		// mqtt.setHost(model.getHost(), model.getPort());
		mqtt.setClientId("programmer");
		mqtt.setKeepAlive((short) 20);
		mqtt.setWillTopic("programmer/alive");
		mqtt.setWillMessage("false");
		mqtt.setClientId("STM32_PROGRAMMER_" + System.currentTimeMillis());

	}

	void onPublish(org.fusesource.mqtt.client.Message msg) {
		try {
			log.info(" received publish message " + msg.getTopic() + ":"
					+ new String(msg.getPayloadBuffer().toByteArray(), "UTF-8"));
			msg.ack();
		} catch (UnsupportedEncodingException e) {
			log.log(Level.SEVERE, "onPublish fails", e);
		}

	}

	void connect(String host, int port) {
		try {
			mqtt.setHost(host, port);
			connection = mqtt.callbackConnection();
			log.info(" connecting to " + host + ":" + port);
			connection.listener(new Listener() {

				public void onDisconnected() {
					log.log(Level.SEVERE, " connection lost");
				}

				public void onConnected() {
					log.log(Level.SEVERE, " connection succeeded.");
				}

				public void onPublish(UTF8Buffer topic, Buffer payload, Runnable ack) {
					try {
						log.info(" recv topic " + topic.toString() + " :" + new String(payload.toByteArray(), "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						log.log(Level.SEVERE, "onPublish ", e);
					}
					ack.run();
				}

				public void onFailure(Throwable value) {
					log.log(Level.SEVERE, " failure ", value);
					connection.disconnect(null);
				}
			});
			connection.connect(new Callback<Void>() {

				@Override
				public void onFailure(Throwable arg0) {
					log.log(Level.SEVERE, "MQTT connection failed ", arg0);
					eb.send("controller", new JsonObject().put("cmd", "connect").put("connected", false));
				}

				@Override
				public void onSuccess(Void arg0) {
					log.info(" MQTT connected.");
					eb.send("controller", new JsonObject().put("cmd", "connect").put("connected", true));
					connection.subscribe(new Topic[] { new Topic("stm32/#", QoS.AT_LEAST_ONCE) },
							new Callback<byte[]>() {

								@Override
								public void onFailure(Throwable arg0) {
									log.log(Level.SEVERE, "subscription failed", arg0);
								}

								@Override
								public void onSuccess(byte[] arg0) {
									log.info("subscribed.");

								}
							});

				}
			});

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	void onEventBusMessage(Message<Object> msg) {
		log.info(" event bus message :" + msg);
		if (msg.body() instanceof String) {
			String cmd = (String) msg.body();
			switch (cmd) {
			case "disconnect": {
				connection.disconnect(null);
				break;
			}
			}
		} else if (msg.body() instanceof JsonObject) {
			JsonObject json = (JsonObject) msg.body();
			if (json.getString("cmd") == "connect") {
				connect(json.getString("host"), json.getInteger("port"));
			} else
				try {
					queue.put(json);
					connection.publish("stm32/in/request", json.toString().getBytes(), QoS.AT_LEAST_ONCE,
							false, null);

				} catch (InterruptedException e) {
					log.log(Level.SEVERE, " queue put failed ", e);
				}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.vertx.core.AbstractVerticle#stop()
	 */
	@Override
	public void stop() throws Exception {
		log.info(" MqttVerticle stopped");
		super.stop();
	}

}

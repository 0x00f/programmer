package be.limero.vertx;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

import be.limero.network.RequestQueue;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class MqttVerticle extends AbstractVerticle implements IMqttActionListener, MqttCallback {
	private final static Logger log = Logger.getLogger(MqttVerticle.class.toString());
	MqttAsyncClient mqtt;
	MqttConnectOptions mqttConnectOptions;
	// static Vertx vertx = Vertx.vertx();
	private static EventBus eb;
	RequestQueue queue = new RequestQueue(300);
	boolean mqttConnected;
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

		super.start();

		log.info("Mqtt verticle started ");
		replies = new HashMap<Integer, Message<Object>>();
		eb = getVertx().eventBus();
		eb.consumer("proxy", msg -> {
			// log.info(" received EB message :" + msg);
			onEventBusMessage(msg);
		});

		// mqtt.setHost(model.getHost(), model.getPort());
		mqttConnectOptions = new MqttConnectOptions();
		mqttConnectOptions.setCleanSession(true);
		mqttConnectOptions.setWill("programmer/alive", "true".getBytes(), 2, false);
		mqttConnectOptions.setKeepAliveInterval(20);
		mqtt = new MqttAsyncClient("tcp://iot.eclipse.org", "stm32-programmer");

	}

	void connect(String host, int port) {
		try {
			log.info(" connecting to " + host + ":" + port);
			mqtt.connect(mqttConnectOptions, this);
		} catch (Exception e) {
			log.log(Level.SEVERE, " connect failed ", e);
		}
	}

	void onEventBusMessage(Message<Object> msg) {
		// log.info(" event bus message :" + msg);
		if (msg.body() instanceof JsonObject) {
			JsonObject json = (JsonObject) msg.body();
			if (json.getString("request") == "connect") {
				connect(json.getString("host"), json.getInteger("port"));
				// mqtt.setCallback(this);
			} else if (json.getString("request") == "disconnect") {
				try {
					mqtt.disconnect();
					eb.send("controller", new JsonObject().put("connected", false).put("reply", "connect"));
				} catch (MqttException e) {
					log.log(Level.SEVERE, " disconnect failed ", e);
				}
			} else { // all other requests to device, if not busy
				try {
					replies.put(json.getInteger("id"), msg);
					if (canSendNext()) sendNext();						
				} catch (Exception e) {
					log.log(Level.SEVERE, " mqtt publish failed ", e);
					log.info("mqtt client isConnected() = " + mqtt.isConnected());
				}
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

	@Override
	public void connectionLost(Throwable exc) {
		log.log(Level.SEVERE, " connection lost ", exc);
		eb.send("controller", new JsonObject().put("reply", "connect").put("connected", false));
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken arg0) {

	}

	@Override
	public void messageArrived(String topic, MqttMessage msg) throws Exception {
/*		log.info(" HOW DID I ARRIVE HERE ? ");
		try {
			String message = new String(msg.getPayload(), "UTF-8");
			log.info(" " + topic + " :" + message);
			if (topic.compareTo("stm32/reply") == 0) {
				JsonObject json = new JsonObject(message);
				log.info(" sending reply on " + json.getInteger("id"));
				replies.get(json.getInteger("id")).reply(json);
				replies.remove(json.getInteger("id"));
				// eb.send("controller", json);
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "onPublish ", e);
		}
*/
	}

	@Override
	public void onFailure(IMqttToken token, Throwable arg1) {
		log.log(Level.SEVERE, " onFailure lost");
		// eb.send("controller", new JsonObject().put("connected",
		// false).put("reply", "connect"));
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
			mqtt.publish("stm32/in/request", json.toString().getBytes(), 0, false, null,
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
		} catch (Exception e) {
			log.log(Level.SEVERE, " send failed.",e);
		}
	}

	@Override
	public void onSuccess(IMqttToken arg0) {
		log.log(Level.SEVERE, " connection succeeded.");
		eb.send("controller", new JsonObject().put("connected", true).put("reply", "connect"));
		try {
			mqtt.subscribe("stm32/#", 0, new IMqttMessageListener() {
				@Override
				public void messageArrived(String topic, MqttMessage msg) throws Exception {
					try {
						String m = new String(msg.getPayload(), "UTF-8");
						log.info("'" + topic + "':" + m);
						if (topic.compareTo("stm32/reply") == 0) {
							log.info("sending reply");
							JsonObject json = new JsonObject(m);
							if (json.containsKey("reply")) {
								log.info("sending reply");
								replies.get(json.getInteger("id")).reply(json);
								replies.remove(json.getInteger("id"));
								if ( json.getInteger("error") !=0 ) {
									log.warning(" error occured , cancelling queue ");
									replies.forEach((k,message)->{
										replies.get(k).fail(-1, "previous command failed");										
									});
									replies.clear();
								}
								else sendNext();
							}
						}
					} catch (Exception e) {
						log.log(Level.SEVERE, "invalid JSON message ", e);
					}
				}
			});
		} catch (MqttException e) {
			log.log(Level.SEVERE, "subscribe failed", e);
		}

	}

}

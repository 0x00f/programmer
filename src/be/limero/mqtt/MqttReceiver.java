package be.limero.mqtt;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import be.limero.common.Bytes;
import be.limero.common.Cbor;
import be.limero.common.LogHandler;

@SuppressWarnings("serial")
public class MqttReceiver extends HashMap<String, MqttEntry> implements MqttCallback, IMqttActionListener {

	private static final Logger log = Logger.getLogger(MqttReceiver.class.getName());
	MqttListener listener=null;
	String host = "iot.eclipse.org";
	int port = 1883;
	MqttAsyncClient mqttClient;
	MqttConnectOptions connOpts;
	boolean connected = false;
	String prefix = "";

	public MqttReceiver() {
		// LogFormatter.Init();
		LogHandler.buildLogger();
		connOpts = new MqttConnectOptions();
		connOpts.setCleanSession(true);
		connOpts.setKeepAliveInterval(60);
	}

	public void publish(String topic, byte[] value) {
		try {
			MqttMessage mm = new MqttMessage();
			mm.setQos(1);
			mm.setPayload(value);
			// mm.setPayload(cbor.addf("B", value).toBytes());
			mqttClient.publish("PUT/" + prefix + topic, mm);
			log.info("PUT/" + prefix + topic + " = " + new Cbor(value).toString()); // PUT/limero314/ESP_00F3A17C/#
		} catch (Exception e) {
			log.log(Level.SEVERE, "PUT call ", e);
		}
	}

	public void publish(String topic, Bytes value) {
		try {
			MqttMessage mm = new MqttMessage();
			mm.setQos(1);
			mm.setPayload(value.bytes());
			// mm.setPayload(cbor.addf("B", value).toBytes());
			mqttClient.publish("PUT/" + prefix + topic, mm);
			log.info("PUT/" + prefix + topic + " = " + new Cbor(value.bytes()).toString()); // PUT/limero314/ESP_00F3A17C/#
		} catch (Exception e) {
			log.log(Level.SEVERE, "PUT call ", e);
		}
	}

	@Override
	public void onFailure(IMqttToken token, Throwable exc) {
		log.warning("connection failed.");
		log.log(Level.SEVERE, token.toString(), exc);
	}

	@Override
	public void onSuccess(IMqttToken arg0) {
		try {
			log.info("connection succeeded.");
			mqttClient.setCallback(this);
			mqttClient.subscribe(prefix + "#", 2);
		} catch (Exception exc) {
			log.log(Level.SEVERE, "onSuccess connection", exc);
		}

	}

	String cborToString(byte[] data) {
		return new Cbor(data).toString();
	}

	@Override
	public void messageArrived(String topic, MqttMessage msg) throws Exception {
		try {
//			log.info("messageArrived " + topic);
			if ( listener != null ) {
				listener.onMessage(topic, new Bytes(msg.getPayload()));
			}
			MqttEntry entry = get(topic);
			String value = cborToString(msg.getPayload());
			log.info(topic + " = " + value + " -- " + new Bytes(msg.getPayload()).toString());
			if (entry == null) {
				put(topic, new MqttEntry(topic, value));
			} else {
				entry.update(value);
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "messageArrived", e);
		}

	}

	@Override
	public void connectionLost(Throwable arg0) {
		log.log(Level.SEVERE, "connection lost");

	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		log.log(Level.INFO, "delivery complete.Exception:" + token.getException());
	}

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	public void connect(String broker) {
		String clientId = "Test" + System.currentTimeMillis();
		MemoryPersistence persistence = new MemoryPersistence();
		try {
			log.info("Starting MQTT Client ");
			mqttClient = new MqttAsyncClient(broker, clientId, persistence);

			log.info("Connecting to broker: " + broker);
			mqttClient.connect(connOpts, this);
			setConnected(false);
			log.info("Connected");

			// mqttClient.disconnect();
			// log.info("Disconnected");
		} catch (MqttException e) {
			log.log(Level.SEVERE, "connection", e);
		}
	}

	public void disconnect() {
		try {

			IMqttToken it = mqttClient.disconnect();
			log.info(" disconnect token :" + it.toString());
			mqttClient.close();
		} catch (Exception e) {
			log.log(Level.SEVERE, "disconnect", e);
		}
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public boolean isConnected() {
		return connected;
	}

	public void setConnected(boolean connected) {
		this.connected = connected;
	}

	/**
	 * @return the prefix
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * @param prefix
	 *            the prefix to set
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	
	

	public MqttListener getListener() {
		return listener;
	}

	public void setListener(MqttListener listener) {
		this.listener = listener;
	}

	public static void main(String[] args) {
		try {
			LogHandler.buildLogger();
			// limero314/ESP_00072740/stm32/progress
			MqttReceiver recv = new MqttReceiver();

			recv.setPrefix("limero314/ESP_00072740/");
			recv.connect("tcp://iot.eclipse.org");
			Thread.sleep(10000);
			recv.publish("limero314/testTopic", new Cbor(100).add("testing"));
			log.info("wakeup from sleep");
			recv.disconnect();

		} catch (Exception e) {
			log.log(Level.SEVERE, "connection", e);
		}

	}

}

package be.limero.vertx;

import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;
import org.fusesource.mqtt.client.Future;
import org.fusesource.mqtt.client.FutureConnection;
import org.fusesource.mqtt.client.Listener;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class MqttVerticle extends AbstractVerticle {
	private final static Logger log = LoggerFactory.getLogger(MqttVerticle.class);
	MQTT mqtt = new MQTT();
	CallbackConnection connection;
	EventBus eb = Vertx.factory.vertx().eventBus();

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.vertx.core.AbstractVerticle#start()
	 */
	@Override
	public void start() throws Exception {

		super.start();

		eb.consumer("proxy", msg -> {
			onMessage(msg);
		});

		mqtt = new MQTT();
		mqtt.setHost("test.mosquitto.org", 1883);
		mqtt.setClientId("programmer");
		mqtt.setKeepAlive((short) 20);
		mqtt.setWillTopic("programmer/alive");
		mqtt.setWillMessage("false");
		
		connection = mqtt.callbackConnection();
		
		connection.listener(new Listener() {

			public void onDisconnected() {
				log.info(" lost connection");
			}

			public void onConnected() {
				log.info(" connected.");
				
			}

			public void onPublish(UTF8Buffer topic, Buffer payload, Runnable ack) {
				// You can now process a received message from a topic.
				// Once process execute the ack runnable.
				log.info(topic.toString()+":"+payload.toString());
				ack.run();
			}

			public void onFailure(Throwable value) {
				connection.disconnect(null); // a connection failure occured.
			}
		});
		connection.connect(new Callback<Void>() {
			public void onFailure(Throwable value) {
				log.info(" failure :" +value); // If we could not connect to the server.
			}

			// Once we connect..
			public void onSuccess(Void v) {

				// Subscribe to a topic
				Topic[] topics = { new Topic("stm32/#", QoS.AT_LEAST_ONCE) };
				connection.subscribe(topics, new Callback<byte[]>() {
					public void onSuccess(byte[] qoses) {
						// The result of the subcribe request.
					}

					public void onFailure(Throwable value) {
						connection.disconnect(null); // subscribe failed.
					}
				});

				// Send a message to a topic
				connection.publish("stm32/in/test", "Hello".getBytes(), QoS.AT_LEAST_ONCE, false, new Callback<Void>() {
					public void onSuccess(Void v) {
						// the pubish operation completed successfully.
					}

					public void onFailure(Throwable value) {
						log.info(" failure :" +value); // If we could not connect to the server.
					}
				});

				// To disconnect..
				connection.disconnect(new Callback<Void>() {
					public void onSuccess(Void v) {
						// called once the connection is disconnected.
					}

					public void onFailure(Throwable value) {
						// Disconnects never fail.
					}
				});
			}
		});
	}

	void onMessage(Message<Object> msg) {
		if ( msg.body() instanceof String) {
			String cmd = (String)msg.body();
			
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

}

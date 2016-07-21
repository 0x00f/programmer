package be.limero.vertx;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;
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

import be.limero.network.Request;
import be.limero.programmer.Bootloader;
import be.limero.programmer.Stm32Model;
import be.limero.programmer.ui.LogHandler;
import be.limero.programmer.ui.Stm32Programmer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;

public class Controller extends AbstractVerticle {
	private final static Logger log = Logger.getLogger(Controller.class.toString());
	static Vertx vertx = Vertx.factory.vertx();
	private final static EventBus eb = vertx.eventBus();
	Stm32Programmer ui;
	Stm32Model model;
	MqttVerticle proxy;
	MQTT mqtt;
	CallbackConnection connection;
	org.fusesource.mqtt.client.Future<Void> future;

	public Controller(Stm32Programmer ui, Stm32Model model) {
		try {
			this.ui = ui;
			this.model = model;
			// proxy = new MqttVerticle();
			LogHandler lh = new LogHandler();
			lh.register(new Callback<String>() {

				@Override
				public void onSuccess(String arg0) {
					model.setLog(model.getLog() + "\n" + arg0);
					ui.updateView();
				}

				@Override
				public void onFailure(Throwable arg0) {
				}

			});

			mqtt = new MQTT();
			mqtt.setHost(model.getHost(), model.getPort());
			mqtt.setClientId("programmer");
			mqtt.setKeepAlive((short) 20);
			mqtt.setWillTopic("programmer/alive");
			mqtt.setWillMessage("false");
			mqtt.setClientId("STM32_PROGRAMMER_" + System.currentTimeMillis());

			eb.consumer("controller", message -> {
				onEbMessage((String) message.body());
				ui.updateView();
			});
			vertx.deployVerticle(this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void send(String s) {
		eb.send("controller", s);
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

	void onEbMessage(String cmd) {
		log.info(" controller received :" + cmd);

		switch (cmd) {
		case "connect": {
			try {
				mqtt.setHost(model.getHost(), model.getPort());
				connection = mqtt.callbackConnection();
				log.info(" connecting to " + model.getHost() + ":" + model.getPort());
				connection.listener(new Listener() {

					public void onDisconnected() {
						log.log(Level.SEVERE, " connection lost");
					}

					public void onConnected() {
						log.log(Level.SEVERE, " connection succeeded.");
					}

					public void onPublish(UTF8Buffer topic, Buffer payload, Runnable ack) {
						try {
							log.info(" recv topic " + topic.toString() + " :"
									+ new String(payload.toByteArray(), "UTF-8"));
						} catch (UnsupportedEncodingException e) {
							// TODO Auto-generated catch
							// block
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
						model.setConnected(false);
					}

					@Override
					public void onSuccess(Void arg0) {
						log.info(" MQTT connected.");
						model.setConnected(true);
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
			break;
		}
		case "disconnect": {
			connection.disconnect(null);
			break;
		}
		case "get": {
			String json = new Request(cmd, Bootloader.Get.request()).toJson().toString();
			log.info(" send json :" + json);
			connection.publish("stm32/in/request", json.getBytes(), QoS.AT_LEAST_ONCE, false, null);
			break;
		}
		case "getId": {
			String json = new Request(cmd, Bootloader.GetId.request()).toJson().toString();
			log.info(" send json :" + json);
			connection.publish("stm32/in/request", json.getBytes(), QoS.AT_LEAST_ONCE, false, null);
			break;
		}
		case "getVersion": {
			String json = new Request(cmd, Bootloader.GetVersion.request()).toJson().toString();
			log.info(" send json :" + json);
			connection.publish("stm32/in/request", json.getBytes(), QoS.AT_LEAST_ONCE, false, null);
			break;
		}
		case "reset": {
			String json = new Request(cmd, Bootloader.Reset.request()).toJson().toString();
			log.info(" send json :" + json);
			connection.publish("stm32/in/request", json.getBytes(), QoS.AT_LEAST_ONCE, false, null);
			break;
		}
		}
	}

	@Override
	public void start(Future<Void> startFuture) {
		log.info("MyVerticle started!");
	}

	@Override
	public void stop(Future<Void> stopFuture) throws Exception {
		log.info("MyVerticle stopped!");
	}

}

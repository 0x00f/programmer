package be.limero.vertx;

import java.util.logging.Logger;

import org.fusesource.mqtt.client.Callback;
import org.fusesource.mqtt.client.CallbackConnection;

import be.limero.network.Request;
import be.limero.programmer.Bootloader;
import be.limero.programmer.Stm32Model;
import be.limero.programmer.ui.LogHandler;
import be.limero.programmer.ui.Stm32Programmer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

public class Controller extends AbstractVerticle {
	private final static Logger log = Logger.getLogger(Controller.class.toString());
	static Vertx vertx = Vertx.vertx();
	private final static EventBus eb = vertx.eventBus();

	Stm32Programmer ui;
	Stm32Model model;
	// MqttVerticle proxy;

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

			eb.consumer("controller", message -> {
				onEbMessage(message.body());
				ui.updateView();
			});
			vertx.deployVerticle(this);
			vertx.deployVerticle(new MqttVerticle());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void send(String s) {
		eb.send("controller", s);
	}

	void onEbMessage(Object msg) {
		log.info(" controller received :" + msg);
		if (msg instanceof JsonObject) {
			JsonObject json = (JsonObject) msg;
			String cmd = json.getString("cmd");
			switch(cmd) {
			case "connect":{
				model.setConnected(json.getBoolean("connected"));
				break;
			}
			}
		} else if (msg instanceof String) {
			String cmd = (String) msg;
			switch (cmd) {
			case "connect": {
				JsonObject json = new JsonObject();
				json.put("cmd", "connect");
				json.put("host", model.getHost());
				json.put("port", model.getPort());
				eb.send("proxy", json);
				break;
			}
			case "disconnect": {
				eb.send("proxy", "disconnect");
				break;
			}

			case "get": {
				eb.send("proxy", new Request(cmd, Bootloader.Get.request()).toJson());
				break;
			}
			case "getId": {
				eb.send("proxy", new Request(cmd, Bootloader.GetId.request()).toJson());
				break;
			}
			case "getVersion": {
				eb.send("proxy", new Request(cmd, Bootloader.GetVersion.request()).toJson());
				break;
			}
			case "read": {
				for (int i = 0; i < 256; i++) {
					eb.send("proxy",
							new Request(cmd, Bootloader.ReadMemory.request(0x08000000 + i * 256, 256)).toJson());
				}
				break;
			}

			case "write": {
				for (int i = 0; i < 256; i++) {
					eb.send("proxy",
							new Request(cmd, Bootloader.WriteMemory.request(0x08000000 + i * 256, new byte[] {}))
									.toJson());
				}
				break;
			}

			case "reset": {
				eb.send("proxy", new Request(cmd, Bootloader.Reset.request()).toJson());
				break;
			}
			}
		}
	}

	@Override
	public void start(Future<Void> startFuture) {
		log.info("ControllerVerticle started!");
	}

	@Override
	public void stop(Future<Void> stopFuture) throws Exception {
		log.info("ControllerVerticle stopped!");
	}

}

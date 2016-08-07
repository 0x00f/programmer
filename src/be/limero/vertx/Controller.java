package be.limero.vertx;

import java.util.Arrays;
import java.util.logging.Logger;

import be.limero.file.FileManager;
import be.limero.network.Request;
import be.limero.programmer.Bootloader;
import be.limero.programmer.Stm32Model;
import be.limero.programmer.ui.LogHandler;
import be.limero.programmer.ui.Stm32Programmer;
import be.limero.util.Bytes;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

public class Controller extends AbstractVerticle implements LogHandler.LogLine {
	private final static Logger log = Logger.getLogger(Controller.class.toString());
	static Vertx vertx = Vertx.vertx();
	private final static EventBus eb = vertx.eventBus();

	Stm32Programmer ui;
	Stm32Model model;
	// MqttVerticle proxy;

	public Controller(Stm32Programmer ui) {
		try {
			this.ui = ui;
			this.model = ui.getStm32Model();
			// proxy = new MqttVerticle();
			LogHandler lh = new LogHandler();
			lh.register(this);

			eb.consumer("controller", message -> {
				onEbMessage(message.body());
				ui.updateView();
			});
			vertx.deployVerticle(this);
			vertx.deployVerticle(new UdpVerticle());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void log(String line) {
		model.setLog(model.getLog() + "\n" + line);
		ui.updateView();

	}

	public void send(String s) {
		eb.send("controller", s);
	}
	// public void askDevice(JsonObject req,
	// Handler<AsyncResult<Message<JsonObject>>> replyHandler) {

	static int nextId = 0;

	public void askDevice(JsonObject req, Handler<JsonObject> replyHandler) {
		DeliveryOptions delOp = new DeliveryOptions();
		delOp.setSendTimeout(5000);
		req.put("id", nextId++);
		eb.send("proxy", req, delOp, resp -> {
			log.info(" handling " + req.getString("request") + " response");
			if (resp.succeeded()) {
				JsonObject json = (JsonObject) resp.result().body();
				int error = json.getInteger("error");
				if (error == 0) {
					replyHandler.handle(json);
				} else {
					log.info(" failed error : " + json.getInteger("error"));
				}
			} else if (resp.failed()) {
				log.info(" failed " + req.getString("request") + " " + resp.cause().getMessage());
			}
		});
	}

	void onEbMessage(Object msg) {
		log.info(" controller received :" + msg);
		if (msg instanceof JsonObject) {
			JsonObject json = (JsonObject) msg;
			if (json.containsKey("reply")) {
				String cmd = json.getString("reply");
				switch (cmd) {
				case "connect": {
					model.setConnected(json.getBoolean("connected"));
					break;
				}
				}
			}
		} else if (msg instanceof String) {
			String cmd = (String) msg;
			switch (cmd) {
			case "connect": {
				askDevice(new JsonObject().put("request", "connect").put("host", model.getHost()).put("port", model.getPort()), reply -> {
					model.setConnected(reply.getBoolean("connected"));

				});
				break;
			}
			case "disconnect": {
				askDevice(new JsonObject().put("request", "disconnect"), reply -> {
					model.setConnected(reply.getBoolean("connected"));

				});

				break;
			}

			case "erase": {
				askDevice(new JsonObject().put("request", "eraseAll"), reply -> {
					log.info(" reply " + reply);

				});
				break;
			}

			case "get": {
				askDevice(new JsonObject().put("request", "get"), reply -> {
					log.info(" reply " + reply);

				});
				break;
			}
			case "getId": {
				askDevice(new JsonObject().put("request", "getId"), reply -> {
					log.info(" reply " + reply);
				});
				break;
			}
			case "getVersion": {
				askDevice(new JsonObject().put("request", "getId"), reply -> {
					log.info(" reply " + reply);
				});
				break;
			}

			case "program": {
				model.setFileMemory(FileManager.loadBinaryFile(model.getBinFile()));
				log.info(" binary image size : " + model.getFileMemory().length);
				int offset = 0;
				while (true) {
					int length = (offset + 256) < model.getFileMemory().length ? 256
							: model.getFileMemory().length - offset;
					if (length <= 0)
						break;
					// log.info(" length :" + length + " offset : " + offset);
					byte[] sector = Arrays.copyOfRange(model.getFileMemory(), offset, offset + length);
					askDevice(new JsonObject().put("request", "writeMemory").put("address", 0x8000000 + offset)
							.put("length", length), reply -> {
								log.info(" reply " + reply);
							});
					offset += 256;
				}
				break;
			}
			case "read": {
				for (int i = 0; i < 256; i++) {
					askDevice(new JsonObject().put("request", "readMemory").put("address", 0x8000000 + i * 156)
							.put("length", 256), reply -> {
								log.info(" reply " + reply);
							});
				}
				break;
			}

			case "write": {

				break;
			}

			case "reset": {
				askDevice(new JsonObject().put("request", "reset"), reply -> {
					log.info(" reply " + reply);
				});break;
			}
			case "go": {
				askDevice(new JsonObject().put("request", "go").put("address", 0x8000000), reply -> {
					log.info(" reply " + reply);
				});
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

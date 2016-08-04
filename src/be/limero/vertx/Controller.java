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
			vertx.deployVerticle(new MqttVerticle());
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

	public void askDevice(JsonObject req, Handler<Bytes> replyHandler) {
		DeliveryOptions delOp = new DeliveryOptions();
		delOp.setSendTimeout(1000);
		eb.send("proxy", req, delOp, resp -> {
			log.info(" handling " + req.getString("request") + " response");
			if (resp.succeeded()) {
				JsonObject json = (JsonObject) resp.result().body();
				int error = json.getInteger("error");
				if (error == 0) {
					Bytes data = new Bytes(json.getBinary("data"));
					replyHandler.handle(data);
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
				JsonObject json = new JsonObject();
				json.put("request", "connect");
				json.put("host", model.getHost());
				json.put("port", model.getPort());
				eb.send("proxy", json);
				break;
			}
			case "disconnect": {
				eb.send("proxy", new JsonObject().put("request", "disconnect"));

				break;
			}

			case "get": {
				askDevice(new Request(cmd, Bootloader.Get.request()).toJson(), data -> {
					Bootloader.Get.parse(data);
					model.setBootloaderVersion(Bootloader.Get.getVersion());
				});
				break;
			}
			case "getId": {
				askDevice(new Request(cmd, Bootloader.GetId.request()).toJson(), data -> {
					Bootloader.GetId.parse(data);
					model.setPid(Bootloader.GetId.getPid());
				});
				break;
			}
			case "getVersion": {
				askDevice(new Request(cmd, Bootloader.GetVersion.request()).toJson(), data -> {
					if (Bootloader.GetVersion.parse(data)) {
						model.setBootloaderVersion(Bootloader.GetVersion.getVersion());
					}
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
					askDevice(new Request(cmd, Bootloader.WriteMemory.request(0x8000000+offset, sector)).toJson(), data -> {
						if (Bootloader.WriteMemory.parse(data)) {
							model.setProgress(length*100/128000);
						}
					});
					offset += 256;
				}
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
			case "go": {
				askDevice(new Request(cmd, Bootloader.Go.request(0x08000000)).toJson(), data -> {
					Bootloader.Go.parse(data);
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

package be.limero.vertx;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.logging.Logger;

import org.omg.CosNaming._NamingContextExtStub;

import be.limero.file.FileManager;
import be.limero.programmer.Stm32Model;
import be.limero.programmer.Stm32Model.Verification;
import be.limero.programmer.ui.LogHandler;
import be.limero.programmer.ui.Stm32Programmer;
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

	DeliveryOptions opt = new DeliveryOptions();

	final int FLASH_START = 0x08000000;
	final int FLASH_SECTOR_SIZE = 256;
	final int FLASH_SIZE = 1024 * 128;

	enum Code {
		OK, FAIL
	};

	class Result {
		Result(Code c, String m) {
			code = c;
			message = m;
		}

		public Code code;
		public String message;
	}

	Stm32Programmer ui;
	Stm32Model model;
	long fileCheckTimer;
	long fileLastModified;
	// MqttVerticle proxy;

	public Controller(Stm32Programmer ui) {
		try {
			this.ui = ui;
			this.model = ui.getStm32Model();
			// proxy = new MqttVerticle();
			LogHandler lh = new LogHandler();
			lh.register(this);

			eb.consumer("controller", message -> {
				onEbMessage(message);
				ui.updateView();
			});
			vertx.deployVerticle(this);
			vertx.deployVerticle(new UdpVerticle());
			fileCheckTimer = vertx.setPeriodic(1000, id -> {
				long fileTime = new File(model.getBinFile()).lastModified();
				if (model.isAutoProgram() & fileTime > fileLastModified) {
					send("controller", "autoProgram", 20000, reply -> {
					}, fail -> {
					});

				}
				fileLastModified = fileTime;
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void send(String s) {
		eb.send("controller", s);
	}

	public void send(JsonObject json) {
		eb.send("controller", json);
	}

	@Override
	public void log(String line) {
		ui.addLog("local", line + "\n");
		// model.setLog(model.getLog() + "\n" + line);
		ui.updateView();

	}

	JsonObject request(String req) {
		return new JsonObject().put("request", req).put("id", nextId++);
	}

	boolean goodResult(AsyncResult<Message<Object>> reply) {
		if (reply.succeeded() && (reply.result() instanceof JsonObject)
				&& ((JsonObject) reply.result()).containsKey("error")
				&& ((JsonObject) reply.result()).getInteger("error") == 0)
			return true;
		return false;
	}

	static int nextId = 0;

	public void askDevice2(int timeout, JsonObject req, Handler<JsonObject> replyHandler) {
		DeliveryOptions delOp = new DeliveryOptions();
		delOp.setSendTimeout(timeout);
		req.put("id", nextId++);
		eb.send("proxy", req, delOp, resp -> {
			// log.info(" handling " + req.getString("request") + " response");
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

	void send(String address, Object message, int timeout, Handler<JsonObject> okHandler,
			Handler<JsonObject> failHandler) {
		opt.setSendTimeout(timeout);
		eb.send(address, message, opt, reply -> {
			if (reply.succeeded()) {
				okHandler.handle((JsonObject) reply.result().body());
			} else {
				if (failHandler != null) {
					failHandler.handle(new JsonObject().put("error", -1));
				}
			}
		});
	}

	public static String byteToHex(byte b) {
		int i = b & 0xFF;
		return Integer.toHexString(i);
	}

	public static String bytesToHex(byte[] bytes) {
		String result = new String();
		for (int i = 0; i < bytes.length; i++) {
			if (i != 0)
				result += " ";
			result += byteToHex(bytes[i]);
		}
		return result;

	}

	void onEbMessage(Message<Object> msg) {

		log.info(" controller received :" + msg.body());
		if (msg.body() instanceof JsonObject) {
			JsonObject json = (JsonObject) msg.body();
			if (json.containsKey("reply")) {
				String cmd = json.getString("reply");
				switch (cmd) {
				case "connect": {
					model.setConnected(json.getBoolean("connected"));
					break;
				}
				}
			} else if (json.containsKey("request")) {
				if (json.getString("request").equals("log")) {
					ui.addLog("remote", new String(json.getBinary("data"), StandardCharsets.UTF_8));
					// model.setUartLog(model.getUartLog() + new
					// String(json.getBinary("data"), StandardCharsets.UTF_8));
					ui.updateView();
				} else if (json.getString("request").equals("settings")) {
					send("proxy", json, 1000, reply -> {
						if (reply.containsKey("baudrate"))
							model.setBaudrate(reply.getInteger("baudrate"));
						msg.reply(reply);
					}, fail -> {
						msg.fail(-1, "failed");
					});
				}
			}
		} else if (msg.body() instanceof String) {
			String cmd = (String) msg.body();
			switch (cmd) {
			case "connect": {
				send("proxy", request("connect").put("host", model.getHost()).put("port", model.getPort()), 1000,
						reply -> {
							model.setConnected(reply.getBoolean("connected"));
							msg.reply(reply);
						}, fail -> {
							msg.fail(-1, "failed");
						});
				break;
			}
			case "disconnect": {
				send("proxy", request("disconnect").put("host", model.getHost()).put("port", model.getPort()), 1000,
						reply -> {
							model.setConnected(reply.getBoolean("connected"));
							msg.reply(reply);
						}, fail -> {
							msg.fail(-1, "failed");
						});
				break;
			}

			case "erase": {
				byte[] cmds = model.getCommands();
				for (int i = 0; i < cmds.length; i++) {
					if (cmds[i] == 0x43)
						send("proxy", request("eraseAll"), 1000, ok -> {
							msg.reply(ok);
						}, fail -> {
							msg.fail(-1, "failed");
						});
					else if (cmds[i] == 0x44)
						send("proxy", request("extendedEraseMemory"), 1000, ok -> {
							msg.reply(ok);
						}, fail -> {
							msg.fail(-1, "failed");
						});
				}
				break;
			}

			case "get": {
				send("proxy", request("get"), 1000, reply -> {
					log.info(" cmds :" + bytesToHex(reply.getBinary("cmds")));
					model.setCommands(reply.getBinary("cmds"));
					msg.reply(reply);
				}, fail -> {
					msg.fail(-1, "failed");
				});
				break;
			}
			case "getId": {
				send("proxy", request("getId"), 1000, reply -> {
					log.info(" chipId :" + Integer.toHexString(reply.getInteger("chipId")));
					msg.reply(reply);
				}, fail -> {
					msg.fail(-1, "failed");
				});
				break;
			}
			case "getVersion": {
				send("proxy", request("getVersion"), 1000, reply -> {
					msg.reply(reply);
				}, fail -> {
					msg.fail(-1, "failed");
				});
				break;
			}

			case "program": {
				model.setFileMemory(FileManager.loadBinaryFile(model.getBinFile()));
				log.info(" binary image size : " + model.getFileMemory().length);
				int offset = 0;
				int fileLength = model.getFileMemory().length;
				while (true) {
					final int sectorLength = (offset + FLASH_SECTOR_SIZE) < fileLength ? FLASH_SECTOR_SIZE
							: model.getFileMemory().length - offset;
					// log.info(" length :" + length + " offset : " + offset);
					if (sectorLength == 0)
						break;
					byte[] sector = Arrays.copyOfRange(model.getFileMemory(), offset, offset + sectorLength);
					send("proxy", request("writeMemory").put("request", "writeMemory")
							.put("address", FLASH_START + offset).put("length", sectorLength).put("data", sector), 1000,
							reply -> {
								// askDevice(50000, new
								// JsonObject().put("request",
								// "writeMemory").put("address", FLASH_START +
								// offset)
								// .put("length", sectorLength).put("data",
								// sector), reply -> {
								int percentage = ((reply.getInteger("address") + FLASH_SECTOR_SIZE - FLASH_START) * 100)
										/ fileLength;
								// log.info(" percentage :" + percentage + "
								// address : "
								// +
								// Integer.toHexString(reply.getInteger("address"))
								// + " length : " + fileLength);
								model.setProgress(percentage);
								ui.updateView();
								// log.info(" reply " + reply);
								msg.reply(reply);
							}, fail -> {
								msg.fail(-1, "failed");
							});
					offset += FLASH_SECTOR_SIZE;
					if (offset > fileLength)
						break;
				}
				break;
			}
			case "read": {
				for (int i = 0; i < FLASH_SIZE; i += FLASH_SECTOR_SIZE) {
					send("proxy", request("readMemory").put("address", FLASH_START + i), 1000, reply -> {
						int percentage = ((reply.getInteger("address") + FLASH_SECTOR_SIZE - FLASH_START) * 100)
								/ FLASH_SIZE;
						model.setProgress(percentage);
						ui.updateView();
						msg.reply(reply);

					}, fail -> {
						msg.fail(-1, "failed");
					});
				}
				break;
			}

			case "verify": {
				model.setFileMemory(FileManager.loadBinaryFile(model.getBinFile()));
				log.info(" binary image size : " + model.getFileMemory().length);
				int binLength = model.getFileMemory().length;

				int offset = 0;
				model.setVerification(Verification.OK);
				while (true) {
					final int sectorLength = (offset + FLASH_SECTOR_SIZE) < binLength ? FLASH_SECTOR_SIZE
							: binLength - offset;
					send("proxy",
							request("readMemory").put("address", FLASH_START + offset).put("length", sectorLength),
							1000, reply -> {

								// askDevice(50000, new
								// JsonObject().put("request",
								// "readMemory").put("address", FLASH_START +
								// offset)
								// .put("length", sectorLength), reply -> {

								int address = reply.getInteger("address");
								byte flashSector[] = reply.getBinary("data");

								int percentage = ((address + FLASH_SECTOR_SIZE - FLASH_START) * 100) / binLength;
								model.setProgress(percentage);
								ui.updateView();

								int off = address - FLASH_START;
								byte binSector[] = Arrays.copyOfRange(model.getFileMemory(), off,
										off + flashSector.length);

								// log.info(" percentage :" + percentage + "
								// address : " + Integer.toHexString(address)
								// + " length : " + binLength + " flash : " +
								// flashSector.length + " bin : "
								// + binSector.length);
								for (int j = 0; j < flashSector.length; j++) {
									if (flashSector[j] != binSector[j]) {
										log.info(" flash differs at 0x" + Integer.toHexString(j + off + FLASH_START)
												+ " flash : 0x" + byteToHex(flashSector[j]) + " bin : 0x"
												+ byteToHex(binSector[j]));
										model.setVerification(Verification.FAIL);
										break;
									}
								}
								msg.reply(reply);
							}, fail -> {
								msg.fail(-1, "failed");
							});
					offset += FLASH_SECTOR_SIZE;
					if (offset > binLength)
						break;
				}
				break;
			}

			case "status": {
				send("proxy", request("status"), 1000, ok -> {
					msg.reply(ok);
				}, fail -> {
					msg.fail(-1, "failed");
				});
				break;
			}

			case "resetBootloader": {
				/*
				 * askDevice(5000, , reply -> { msg.reply(new
				 * JsonObject().put("error", 0)); });
				 */
				send("proxy", request("resetBootloader"), 1000, ok -> {
					send("controller", "get", 1000, ok2 -> {
						send("controller", "getId", 1000, ok3 -> {
							send("controller", "getVersion", 1000, ok4 -> {
								msg.reply(ok4);
							}, fail -> {
								msg.fail(-1, "failed");
							});
						}, fail -> {
							msg.fail(-1, "failed");
						});
					}, fail -> {
						msg.fail(-1, "failed");
					});
				}, fail -> {
					msg.fail(-1, "failed");
				});
				break;
			}
			case "autoProgram": {
				/*
				 * askDevice(5000, , reply -> { msg.reply(new
				 * JsonObject().put("error", 0)); });
				 */
				send("controller", "resetBootloader", 1000, ok -> {
					send("controller", "erase", 1000, ok2 -> {
						send("controller", "program", 1000, ok3 -> {
							send("controller", "resetFlash", 1000, ok4 -> {
								msg.reply(ok4);
							}, fail -> {
								msg.fail(-1, "failed");
							});
						}, fail -> {
							msg.fail(-1, "failed");
						});
					}, fail -> {
						msg.fail(-1, "failed");
					});
				}, fail -> {
					msg.fail(-1, "failed");
				});
				break;
			}
			case "resetFlash": {
				send("proxy", request("resetFlash"), 1000, ok -> {
					msg.reply(ok);
				}, fail -> {
					msg.fail(-1, "failed");
				});
				break;
			}
			case "goFlash": {
				send("proxy", request("goFlash").put("address", FLASH_START), 1000, ok -> {
					msg.reply(ok);
				}, fail -> {
					msg.fail(-1, "failed");
				});
				break;
			}
			case "baudrate": {
				send("proxy", request("settings").put("baudrate", 460800), 1000, ok -> {
					msg.reply(ok);
				}, fail -> {
					msg.fail(-1, "failed");
				});
				break;
			}
			default: {
				log.info("unknown command");
				;
			}
			}
		} else

		{
			log.info("unknown message : " + msg.body());
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

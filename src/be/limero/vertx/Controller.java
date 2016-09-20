package be.limero.vertx;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
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

	enum Stm32Id {
		STM32_CHIPID_F1_MEDIUM(0x410) //
		, STM32_CHIPID_F2(0x411) //
		, STM32_CHIPID_F1_LOW(0x412) //
		, STM32_CHIPID_F3(0x422) //
		, STM32_CHIPID_F37(0x432) //
		, STM32_CHIPID_F4(0x413) //
		, STM32_CHIPID_F1_HIGH(0x414) //
		, STM32_CHIPID_L1_MEDIUM(0x416) //
		, STM32_CHIPID_F1_CONN(0x418) //
		, STM32_CHIPID_F1_VL_MEDIUM(0x420) //
		, STM32_CHIPID_F1_VL_HIGH(0x428) //
		, STM32_CHIPID_F1_XL(0x430) //
		, STM32_CHIPID_F0(0x440); //
		int chipId;

		Stm32Id(int x) {
			chipId = x;
		}

		static String findName(int x) {
			for (Stm32Id id : Stm32Id.values())
				if (x == id.chipId)
					return id.name();
			return "Unknown chipId : " + Integer.toHexString(x);
		}
	};

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
			loadConfig();
			ui.updateView();
			eb.consumer("controller", message -> {
				onEbMessage(message);
				ui.updateView();
			});
			vertx.deployVerticle(this);
			vertx.deployVerticle(new MqttVerticle2());
			fileCheckTimer = vertx.setPeriodic(1000, id -> {
				long fileTime = new File(model.getBinFile()).lastModified();
				if (model.isAutoProgram() & fileTime > fileLastModified) {
					send("autoProgram");
				}
				fileLastModified = fileTime;
			});
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void send(String s) {
		eb.send("controller", request(s));
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

	public JsonObject request(String req) {
		return new JsonObject().put("request", req).put("id", nextId++);
	}

	boolean goodResult(AsyncResult<Message<Object>> reply) {
		Object body = reply.result().body();
		if (reply.succeeded() && (body instanceof JsonObject) && ((JsonObject) body).containsKey("error")
				&& ((JsonObject) body).getInteger("error") == 0)
			return true;
		return false;
	}

	static int nextId = 0;

	void send(String address, Object message, int timeout, Handler<JsonObject> okHandler,
			Handler<JsonObject> failHandler) {
		opt.setSendTimeout(timeout);
		eb.send(address, message, opt, reply -> {
			if (reply.succeeded()) {
				if (goodResult(reply))
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
				switch (json.getString("reply")) {
				case "connect": {
					model.setConnected(json.getBoolean("connected"));
					break;
				}
				case "settings" :{
					model.getSettings().put(json.getString("topic"), json.getString("message"));
					break;
				}
				}
			} else if (json.containsKey("event")) {
				switch (json.getString("event")) {
				case "data": {
					ui.addLog("remote", json.getString("message"));
					break;
				}
				}
			} else  if (json.containsKey("request")) {
				switch (json.getString("request")) {
				case "connect": {
					send("proxy", request("connect").put("host", model.getHost()).put("port", model.getPort()), 5000,
							reply -> {
								model.setConnected(reply.getBoolean("connected"));
								msg.reply(reply);
							}, fail -> {
								msg.fail(-1, "failed");
								log.info(" connect failed " + msg.toString());
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
				case "log": {
					ui.addLog("remote", json.getString("data")+"\n");
					ui.updateView();
					break;
				}
				case "settings": {
					send("proxy", json, 1000, reply -> {
						if (reply.containsKey("baudrate"))
							model.setBaudrate(reply.getInteger("baudrate"));
						msg.reply(reply);
					}, fail -> {
						msg.fail(-1, "failed");
					});
					break;
				}
				/*
				 * case "connect": { send("proxy",
				 * request("connect").put("host", model.getHost()).put("port",
				 * model.getPort()), 1000, reply -> {
				 * model.setConnected(reply.getBoolean("connected"));
				 * msg.reply(reply); }, fail -> { msg.fail(-1, "failed");
				 * log.info(" connect failed " + msg.toString()); }); break; }
				 * case "disconnect": { send("proxy",
				 * request("disconnect").put("host",
				 * model.getHost()).put("port", model.getPort()), 1000, reply ->
				 * { model.setConnected(reply.getBoolean("connected"));
				 * msg.reply(reply); }, fail -> { msg.fail(-1, "failed"); });
				 * break; }
				 */
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
						model.setDeviceInfo(Stm32Id.findName(reply.getInteger("chipId")));
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

						if (sectorLength == 0)
							break;
						byte[] sector = Arrays.copyOfRange(model.getFileMemory(), offset, offset + sectorLength);
						send("proxy",
								request("writeMemory").put("address", FLASH_START + offset)//
										.put("length", sectorLength).put("data", sector),
								20000, //
								reply -> {
									int percentage = ((reply.getInteger("address") + FLASH_SECTOR_SIZE - FLASH_START)
											* 100) / fileLength;
									log.info(" percentage : " + percentage + " address : "
											+ Integer.toHexString(reply.getInteger("address"))//
											+ " start : " + Integer.toHexString(FLASH_START) //
											+ " end : " + Integer.toHexString(FLASH_START + fileLength) //
									);
									model.setProgress(percentage);
									ui.updateView();
									msg.reply(reply);
								}, //
								fail -> {
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
						send("proxy", request("readMemory").put("address", FLASH_START + i).put("length", 256), 1000, reply -> {
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
								20000, reply -> {

									int address = reply.getInteger("address");
									byte flashSector[] = reply.getBinary("data");

									int percentage = ((address + FLASH_SECTOR_SIZE - FLASH_START) * 100) / binLength;
									model.setProgress(percentage);
									ui.updateView();

									int off = address - FLASH_START;
									byte binSector[] = Arrays.copyOfRange(model.getFileMemory(), off,
											off + flashSector.length);

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
						model.setStatus(ok);
						msg.reply(ok);
					}, fail -> {
						msg.fail(-1, "failed");
					});
					break;
				}

				case "resetBootloader": {
					send("proxy", request("resetBootloader"), 1000, ok -> {
						send("controller", request("get"), 1000, ok2 -> {
							send("controller", request("getId"), 1000, reply3 -> {
								model.setId(reply3.getInteger("chipId"));
								model.setDeviceInfo(Stm32Id.findName(reply3.getInteger("chipId")));
								send("controller", request("getVersion"), 1000, ok4 -> {
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

					send("controller", request("resetBootloader"), 1000, ok -> {
						send("controller", request("erase"), 1000, ok2 -> {
							send("controller", request("program"), 1000, ok3 -> {
								send("controller", request("resetFlash"), 1000, ok4 -> {
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
					send("proxy", request("settings").put("baudrate", model.getBaudrate()), 1000, ok -> {
						msg.reply(ok);
					}, fail -> {
						msg.fail(-1, "failed");
					});
					break;
				}

				default: {
					log.info("unknown command");
				}

				}
			} else { // not request or reply
				log.info("unknown JSON message , no request or reply : " + msg.body());
			}
		} else { // no string or jsonobject
			log.info("unknown message format : " + msg.body() + " " + msg.body().getClass().getName());
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

	public void saveConfig() {
		try {
			JsonObject config = new JsonObject();
			config.put("mqtt.host", model.getHost());
			config.put("mqtt.port", model.getPort());
			config.put("file.bin", model.getBinFile());
			config.put("mqtt.prefix", model.getPrefix());
			config.put("uart.baudrate", model.getBaudrate());
			String configuration = config.encodePrettily();
			PrintWriter writer = new PrintWriter("wibo.cfg", "UTF-8");
			writer.println(configuration);
			writer.close();
		} catch (Exception ex) {
			log.warning(" save Config failed : " + ex.getMessage());
		}

	}

	String getValue(JsonObject json, String key, String defValue) {
		if (json.containsKey(key))
			return json.getString(key);
		else
			return defValue;
	}

	int getValue(JsonObject json, String key, int defValue) {
		if (json.containsKey(key))
			return json.getInteger(key);
		else
			return defValue;
	}

	public void loadConfig() {
		String everything;
		try {
			BufferedReader br = new BufferedReader(new FileReader("wibo.cfg"));
			try {
				StringBuilder sb = new StringBuilder();
				String line = br.readLine();

				while (line != null) {
					sb.append(line);
					sb.append(System.lineSeparator());
					line = br.readLine();
				}
				everything = sb.toString();
			} finally {
				br.close();
			}
			JsonObject config = new JsonObject(everything);

			model.setHost(getValue(config, "mqtt.host", "test.mosquitto.org"));
			model.setPort(getValue(config, "mqtt.port", 1883));
			model.setBinFile(getValue(config, "file.bin", "c:\\"));
			model.setBaudrate(getValue(config, "uart.baudrate", 460800));
			model.setPrefix(getValue(config, "mqtt.prefix", "wibo1/bootloader"));
		} catch (Exception ex) {
			log.warning(" load Config failed : " + ex.getMessage());
		}
	}

}

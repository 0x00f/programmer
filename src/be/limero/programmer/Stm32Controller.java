package be.limero.programmer;

import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;

import be.limero.common.Bytes;
import be.limero.common.Cbor;
import be.limero.mqtt.MqttListener;
import be.limero.mqtt.MqttReceiver;
import be.limero.programmer.commands.Stm32Reset;
import be.limero.programmer.commands.Stm32EnterBootloader;
import be.limero.programmer.commands.Stm32GetId;
import be.limero.programmer.commands.Stm32GetVersionCommands;
import be.limero.programmer.commands.Stm32Go;
import be.limero.programmer.commands.Stm32Msg;
import be.limero.programmer.commands.Stm32ReadoutProtect;
import be.limero.programmer.ui.Stm32Programmer;

public class Stm32Controller implements MqttListener {
	private static final Logger log = Logger.getLogger(Stm32Controller.class.getName());
	// start Mqtt exchange thread
	Stm32Model model;
	Stm32Programmer ui;
	MqttReceiver mqtt;
	HashMap<Integer,Stm32Msg> queue;

	public Stm32Controller(Stm32Programmer frame) {
		model = new Stm32Model();
		mqtt = new MqttReceiver();
		ui = frame;
		ui.updateView();
	}

	public void getModelFromView() {
		model.setMqttConnectionString(ui.getTxtMqttConnection().getText());
		model.setMqttPrefix(ui.getTxtMqttPrefix().getText());
	}

	public void connect() {
		getModelFromView();
		mqtt.setPrefix(model.getMqttPrefix());
		mqtt.connect(model.getMqttConnectionString());
		mqtt.setListener(this);
	}

	public void loadFile() {

	}

	public void program() {

	}

	public void reset() {
		sendCommand(new Stm32Reset());
	}

	public void enterBootloader() {
		sendCommand(new Stm32EnterBootloader());
	}

	public void verify() {

	}

	public void go() {
		sendCommand(new Stm32Go(0x80000000));
	}

	public void getId() {
		sendCommand(new Stm32GetId());
	}

	public void getVersionCommands() {
		sendCommand(new Stm32GetVersionCommands());
	}
	
	public void sendCommand(Stm32Msg  msg) {
		queue.put(msg.messageId, msg);
		mqtt.publish("stm32/cmd", msg);
	}

	@Override
	public void onMessage(String topic, Bytes value) {
		Cbor cbor = new Cbor(value.bytes());
		if (topic.endsWith("stm32/status")) {
			String status = cbor.getString();
			model.setStatus(status);
		} else if (topic.endsWith("stm32/progress")) {
			Integer progress = cbor.getInteger();
			model.setProgress(progress);
		} else if (topic.endsWith("stm32/log")) {
			String s = cbor.getString();
			model.log(s);
		} else if(topic.endsWith("stm32/cmd")) {
			Stm32Msg msg=new Stm32Msg(value.bytes());
			msg.parse();
			// find corresponding Stm32Msg
			Stm32Msg msg2 = queue.get(msg.messageId);
			// call handle(model)
		}
		ui.updateView();
	}

	public Stm32Model getModel() {
		return model;
	}

	public void setModel(Stm32Model model) {
		this.model = model;
	}

	public Stm32Programmer getUi() {
		return ui;
	}

	public void setUi(Stm32Programmer ui) {
		this.ui = ui;
	}

}

package be.limero.programmer;

import java.util.logging.Logger;

import be.limero.common.Bytes;
import be.limero.common.Cbor;
import be.limero.mqtt.MqttListener;
import be.limero.mqtt.MqttReceiver;
import be.limero.programmer.commands.Stm32Reset;
import be.limero.programmer.commands.Stm32EnterBootloader;
import be.limero.programmer.commands.Stm32Go;
import be.limero.programmer.commands.Stm32ReadoutProtect;
import be.limero.programmer.ui.Stm32Programmer;

public class Stm32Controller implements MqttListener {
	private static final Logger log = Logger.getLogger(Stm32Controller.class.getName());
	// start Mqtt exchange thread
	Stm32Model model;
	Stm32Programmer ui;
	MqttReceiver mqtt;

	public Stm32Controller(Stm32Programmer frame) {
		model = new Stm32Model();
		mqtt = new MqttReceiver();
		ui = frame;
		ui.updateView();
	}
	
	public void getModelFromView(){
		model.setMqttConnectionString(ui.getTxtMqttConnection().getText());
		model.setMqttPrefix(ui.getTxtMqttPrefix().getText());
	}
	
	public void connect(){
		getModelFromView();
		mqtt.setPrefix(model.getMqttPrefix());
		mqtt.connect(model.getMqttConnectionString());
		mqtt.setListener(this);
	}
	
	public void loadFile() {
		
	}
	
	public void program() {
		
	}
	
	public void reset(){
		Cbor msg= new Stm32Reset();
		mqtt.publish("stm32/cmd", msg);
	}
	
	public void enterBootloader(){
		Cbor msg=new Stm32EnterBootloader();
		mqtt.publish("stm32/cmd", msg);
	}
	
	public void verify(){
		
	}
	
	public void go(){
		new Stm32Go(0x4000000);
	}

	@Override
	public void onMessage(String topic, Bytes value) {
		Cbor cbor=new Cbor(value.bytes());
		if ( topic.endsWith("stm32/status")) {
			String status=cbor.getString();
			model.setStatus(status);
		} else if ( topic.endsWith("stm32/progress")) {
			Integer progress=cbor.getInteger();
			model.setProgress(progress);
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

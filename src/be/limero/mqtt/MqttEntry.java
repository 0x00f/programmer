package be.limero.mqtt;

import java.util.Date;

public class MqttEntry {

	public String topic;
	public String value;
	public Date updated;
	public int updates;

	MqttEntry(String _topic, String _value) {
		topic = _topic;
		value = _value;
		updated = new Date();
		updates = 1;
	}

	void update(String _value) {
		value = _value;
		updated = new Date();
		updates++;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}

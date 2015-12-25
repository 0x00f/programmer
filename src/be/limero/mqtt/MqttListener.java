package be.limero.mqtt;

import be.limero.common.Bytes;

public interface MqttListener {
	void onMessage(String topic,Bytes value);
}

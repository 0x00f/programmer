package be.limero.common;

public interface MessageReceiver {
	void onMessage(Bytes message);
}

package be.limero.socket;

import java.util.logging.Level;
import java.util.logging.Logger;

import be.limero.common.Bytes;
import be.limero.common.Cbor;
import be.limero.common.LogHandler;
import be.limero.common.MessageReceiver;
import be.limero.common.Slip;

public class SocketTester implements MessageReceiver {
	private static final Logger log = Logger.getLogger(SocketTester.class.getName());

	public SocketTester() {
		
	}
	
	void test(){
		SocketClient sc = new SocketClient();
		sc.connect("192.168.0.226", 2323);
		Cbor cbor=new Cbor(100);
		cbor.addField(Field.MESSAGE_ID.ordinal(),1234);
		cbor.addField(Field.ERRNO.ordinal(),1);
		log.info(" cbor "+cbor.toString());
		sc.send(Slip.encode(cbor));
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sc.disconnect();
	}
	
	@Override
	public void onMessage(Bytes message) {
		log.info("message received "+message.toString());
	}
	
	public static void main(String[] args) {
		LogHandler.buildLogger();
		SocketTester tester=new SocketTester();
		while (true){
			try {

				tester.test();
				Thread.sleep(5000);
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
	}
}

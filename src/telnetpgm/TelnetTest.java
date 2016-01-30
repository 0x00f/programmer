package telnetpgm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import be.limero.common.Bytes;
import be.limero.common.LogHandler;

public class TelnetTest {
	private static final Logger log = Logger.getLogger(TelnetTest.class.getName());
	Socket socket;
	OutputStream os;
	InputStream is;

	TelnetTest() {

	}

	void connect(String hostname, int portNumber) {

		try {
			socket = new Socket(hostname, portNumber);
			os = socket.getOutputStream();
			is = socket.getInputStream();
			/*
			 * Thread thread = new Thread(new Runnable() {
			 * 
			 * @Override public void run() { while (true) { try { log.info(
			 * "data in :" + is.read()); } catch (IOException e) {
			 * log.log(Level.WARNING, "send failed", e); } } } });
			 */
		} catch (Exception e) {
			log.log(Level.WARNING, "connect failed", e);
		}
	}

	void send(byte[] data) {
		try {
			// log.info(new Bytes(data).toString());
			os.write(data);
		} catch (IOException e) {
			log.log(Level.WARNING, "send failed", e);
		}
	}

	byte[] recv() {
		try {
			while (is.available() > 0)
				is.read();
			// log.info("data in :" + is.read());
		} catch (IOException e) {
			log.log(Level.WARNING, "send failed", e);
		}
		return null;
	}

	void disconnect() {
		try {
			is.close();
			os.close();
			socket.close();
		} catch (IOException e) {
			log.log(Level.WARNING, "disconnect failed", e);
		}
	}

	public static void main(String[] args) {
		LogHandler.buildLogger();
		while (true) {
			try {
				for (int i = 0; i < 1000000; i++) {
					TelnetTest tt = new TelnetTest();
					tt.connect("192.168.0.228", 2323);
					for (int j = 0; j < 2; j++) {
						log.info(i + " " + j + " " + tt.socket.getLocalPort() + " "
								+ tt.socket.getRemoteSocketAddress().toString());
						tt.send(("" + j).getBytes());
						byte[] data = tt.recv();
						Thread.sleep(500);
					}
					// tt.send(new byte[] { 0x7F, 0x7F });
					// tt.send(new byte[] { 0, -1 });
					tt.disconnect();
					Thread.sleep(100);
				}
			} catch (Exception e) {
				log.log(Level.WARNING, "send failed", e);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
	}

}

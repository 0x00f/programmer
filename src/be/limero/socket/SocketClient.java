package be.limero.socket;

import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;

import be.limero.common.Bytes;
import be.limero.common.LogHandler;
import be.limero.common.MessageReceiver;
import be.limero.common.Slip;
import be.limero.common.Str;
import be.limero.programmer.commands.Stm32GetVersionCommands;

public class SocketClient {
	Socket socket = null;
	DataOutputStream os = null;
	DataInputStream is = null;
	private static final Logger log = Logger.getLogger(SocketClient.class.getName());
	MessageReceiver messageReceiver = null;
	RequestHandler handler = null;

	class RequestHandler extends Thread {
		private Socket socket;
		BufferedReader in;
		Slip slip;

		RequestHandler(Socket socket) {
			try {
				this.socket = socket;
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				slip = new Slip(1024);
			} catch (Exception e) {
				log.log(Level.SEVERE, "RequestHandler", e);
			}
		}

		@Override
		public void run() {
			try {
				while (true) {
					Bytes bytes;
					bytes = slip.fill((byte) in.read());
					if (bytes != null) {
						log.info("recv : " + bytes.toString());
						messageReceiver.onMessage(bytes);
					}

				}
			} catch (Exception e) {
				log.log(Level.SEVERE, "stopped ?", e);
			}
		}
	}

	void connect(String host, int port) {
		try {
			socket = new Socket(host, port);
			os = new DataOutputStream(socket.getOutputStream());
			is = new DataInputStream(socket.getInputStream());
			handler = new RequestHandler(socket);
			handler.start();
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host: hostname");
		} catch (IOException e) {
			log.log(Level.SEVERE, "connect", e);
		}
	}

	void send(Bytes bytes) {
		try {
			os.write(bytes.bytes(), 0, bytes.length());
			log.info("send : " + bytes.toString());
		} catch (Exception e) {
			log.log(Level.SEVERE, "send", e);
		}
	}

	void onMessage(MessageReceiver receiver) {

	}

	void disconnect() {
		try {
			is.close();
			os.close();
			socket.close();
			handler.join();
		} catch (Exception e) {
			log.log(Level.SEVERE, "disconnect", e);
		}

	}

	public static void main(String[] args) {
		LogHandler.buildLogger();
		Stm32Msg msg = new Stm32Msg();
		log.info(msg.toJson());
		log.info(msg.toString());
		msg.fromJson(msg.toJson());
		log.info(msg.toJson());
		log.info(msg.toString());
		while (true) {
			SocketClient sc = new SocketClient();
			sc.connect("192.168.0.121", 2323);

			// from JSON to object

			sc.send(Slip.encode(new Str(new Stm32Msg().toJson())));
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			sc.disconnect();
		}

	}
}

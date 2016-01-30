package be.limero.socket;

import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import be.limero.common.Bytes;
import be.limero.common.LogHandler;
import be.limero.programmer.commands.Stm32GetVersionCommands;

public class SocketClient {
	Socket socket = null;
	DataOutputStream os = null;
	DataInputStream is = null;
	private static final Logger log = Logger.getLogger(SocketClient.class.getName());

	void connect(String host, int port) {
		try {
			socket = new Socket(host, port);
			os = new DataOutputStream(socket.getOutputStream());
			is = new DataInputStream(socket.getInputStream());

		} catch (UnknownHostException e) {
			System.err.println("Don't know about host: hostname");
		} catch (IOException e) {
			log.log(Level.SEVERE, "connect", e);
		}
	}

	void send(Bytes bytes){
		try{
			os.write(bytes.bytes(), 0, bytes.length());
		} catch (Exception e) {
			log.log(Level.SEVERE, "send", e);
		}
	}

	void disconnect() {
		try {
			is.close();
			os.close();
			socket.close();
		} catch (Exception e) {
			log.log(Level.SEVERE, "disconnect", e);
		}

	}

	public static void main(String[] args) {
		LogHandler.buildLogger();
		SocketClient sc=new SocketClient();
		sc.connect("192.168.0.226", 2323);
		sc.send(new Bytes());
		sc.disconnect();
		
	}
}

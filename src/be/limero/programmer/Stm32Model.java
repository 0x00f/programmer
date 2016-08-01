package be.limero.programmer;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class Stm32Model {

	String log;
	String status;
	String host;
	int port;
	byte bootloaderVersion;
	short chipId;
	byte[] commands;
	int id;
	Integer progress;
	Boolean connected;
	String binFile;
	byte[] flashMemory;
	byte[] fileMemory;

	public Stm32Model() {

		progress = 0;
		status = "undefined status";
		bootloaderVersion = 0;
		connected = false;
		log = "";
		id = 0;
		flashMemory = new byte[0x20000];
		Arrays.fill(flashMemory, (byte) 0xFF);
	}
}

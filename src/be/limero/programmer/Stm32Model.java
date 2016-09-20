package be.limero.programmer;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import be.limero.programmer.ui.SettingsTable;
import io.vertx.core.json.JsonObject;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Data
public class Stm32Model {

	String log;
	String uartLog;
	String host;
	String deviceInfo;
	int port;
	byte bootloaderVersion;
	short pid;
	byte[] commands;
	int id;
	Integer progress;
	Boolean connected;
	String binFile;
	byte[] flashMemory;
	byte[] fileMemory;
	int heapSize;
	boolean autoProgram;
	int baudrate;
	JsonObject status;
	SettingsTable settings;
	String prefix;

	public enum Verification {
		NA, OK, FAIL
	};

	Verification verification;

	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

	public void addPropertyChangeListener(PropertyChangeListener pcl) {
		pcs.addPropertyChangeListener(pcl);
	}

	public void removePropertyChangeListener(PropertyChangeListener pcl) {
		pcs.removePropertyChangeListener(pcl);
	}

	public Stm32Model() {

		progress = 0;
		status = new JsonObject();
		bootloaderVersion = 0;
		connected = false;
		log = "";
		id = 0;
		flashMemory = new byte[0x20000];
		Arrays.fill(flashMemory, (byte) 0xFF);
		verification=Verification.NA;
		autoProgram=false;
		settings = new SettingsTable();
	}
}

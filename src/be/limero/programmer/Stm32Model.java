package be.limero.programmer;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import lombok.Getter;
import lombok.Setter;

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

	public String getBootloaderVersion() {
		return "" + (bootloaderVersion >> 4) + "." + (bootloaderVersion & 0xF);
	}

	public String getInfo() {
		return "bootloader version : " + getBootloaderVersion() + " ID : " + getId();
	}

	public void setBootloaderVersion(byte bootLoaderVersion) {
		this.bootloaderVersion = bootLoaderVersion;
	}

	public short getChipId() {
		return chipId;
	}

	public void setChipId(short chipId) {
		this.chipId = chipId;
	}

	public byte[] getCommands() {
		return commands;
	}

	public void setCommands(byte[] commands) {
		this.commands = commands;
	}

	public String getBinFile() {
		return binFile;
	}

	public void setBinFile(String binFile) {
		this.binFile = binFile;
	}

	public Integer getProgress() {
		return progress;
	}

	public void setProgress(Integer progress) {
		this.progress = progress;
	}

	public Boolean getConnected() {
		return connected;
	}

	public void setConnected(Boolean connected) {
		this.connected = connected;
	}

	public void log(String str) {
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
		log += "\n" + sdf.format(new Date()) + " | " + str;
	}

	public String getId() {
		return Integer.toHexString(id);
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getLog() {
		return log;
	}

	public void setLog(String log) {
		this.log = log;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * @return the flashMemory
	 */
	public byte[] getFlashMemory() {
		return flashMemory;
	}

	/**
	 * @param flashMemory the flashMemory to set
	 */
	public void setFlashMemory(byte[] flashMemory) {
		this.flashMemory = flashMemory;
	}

	/**
	 * @return the fileMemory
	 */
	public byte[] getFileMemory() {
		return fileMemory;
	}

	/**
	 * @param fileMemory the fileMemory to set
	 */
	public void setFileMemory(byte[] fileMemory) {
		this.fileMemory = fileMemory;
	}


	// ________________________________________________________________________________

}

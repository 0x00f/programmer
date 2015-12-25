package be.limero.programmer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import be.limero.common.Util;

public class Stm32Model {
	String log;
	String status;
	String mqttConnectionString;
	String MqttPrefix;
	byte bootloaderVersion;
	short chipId;
	byte[] commands;
	Integer progress;
	Boolean connected;
	
	String binFile;
	Vector<MemoryPage> binPages;
	
	MemoryImage binFileImage;
	MemoryImage memoryImage;
	
	
	
	public Stm32Model(){
		progress=0;
		status="undefined status";
		bootloaderVersion=0;
		connected = false;
		log="";
	}
	
	
	public static void main(String[] args) {

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



	public String getMqttPrefix() {
		return MqttPrefix;
	}



	public void setMqttPrefix(String mqttPrefix) {
		MqttPrefix = mqttPrefix;
	}



	public String getBootloaderVersion() {
		return Util.bytesToHex(new byte[]{bootloaderVersion});
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


	public String getMqttConnectionString() {
		return mqttConnectionString;
	}


	public void setMqttConnectionString(String mqttConnectionString) {
		this.mqttConnectionString = mqttConnectionString;
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
	
	public void log(String str){
		SimpleDateFormat sdf=new SimpleDateFormat("HH:mm:ss.SSS");
		log += "\n" + sdf.format(new Date() )+" | "+str;
	}
	


	
	//________________________________________________________________________________
	

}

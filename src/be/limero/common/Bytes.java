package be.limero.common;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.logging.Logger;

import be.limero.programmer.Stm32Cmd;

/**
 *
 * @author lieven2
 */
public class Bytes {
	private static final Logger log = Logger.getLogger(Stm32Cmd.class.getName());
	byte[] data;
	int capacity;
	int offset;
	int used;
	
	public Bytes() {
		log.warning("empty constructor called");
	}

	public Bytes(int size) {
		data = new byte[size];
		offset = 0;
		capacity = size;
		used = 0;
	}
	
	public void resize(int size){
		data = new byte[size];
		offset = 0;
		capacity = size;
		used = 0;
	}

	public Bytes(byte[] b) {
		data = b;
		offset = 0;
		capacity = b.length;
		used = b.length;
	}

	public Bytes clear(){
		used=0;
		offset=0;
		return this;
	}
	public void write(byte b) {
		if (offset < capacity) {
			data[offset] = b;
			offset++;
			used++;
		}
	}
	
	public void write(byte[] b){
		for(int i=0;i<b.length;i++){
			write(b[i]);
		}
	}

	public void write(int b) {
		write((byte) b);
	}

	public void write(long b) {
		write((byte) b);
	}

	public byte read() {
		if (offset < used) {
			return data[offset++];
		}
		return '-';
	}

	public boolean hasData() {
		if (offset < used) {
			return true;
		}
		return false;
	}
	
	public boolean hasSpace() {
		if (used < capacity) {
			return true;
		}
		return false;
	}

	public void offset(int l) {
		offset = l;
	}

	public int offset() {
		return offset;
	}
	
	public int used(){
		return used;
	}

	void move(int dist) {
		if (offset + dist < used) {
			offset += dist;
		}
	}

	public int length() {
		return used;
	}
	
	public byte peek(int offset){
		return data[offset];
	}

	public byte[] bytes() {
		byte[] res = new byte[length()];
		for (int i = 0; i < length(); i++) {
			res[i] = data[i];
		}
		return res;
	}
	
	public Bytes sub(int offset,int length) {
		Bytes bytes=new Bytes(length);
		offset(offset);
		while(bytes.hasSpace()) 
			bytes.write(read());
		return bytes;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < used; i++) {
			if (i != 0)
				str.append(' ');
			str.append(String.format("%02X", data[i]));
		}
		return str.toString();
	}

}

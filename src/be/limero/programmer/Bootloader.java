package be.limero.programmer;

import java.util.HashMap;

import be.limero.util.Bytes;
import lombok.Getter;

/*
3.1 Get command . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 8
3.2 Get Version & Read Protection Status command . . . . . . . . . . . . . . . . . . . 10
3.3 Get ID command . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .11
3.4 Read Memory command . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 13
3.5 Go command . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 16
3.6 Write Memory command . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 18
3.7 Erase Memory command . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 21
3.8 Extended Erase Memory command . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 24
3.9 Write Protect command . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 27
3.10 Write Unprotect command . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 30
3.11 Readout Protect command . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 31
3.12 Readout Unprotect command . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . 33
*/

public class Bootloader {

	static final byte X_WAIT_ACK = 0x40; // wait for ACK
	static final byte X_SEND = 0x41; // send fixed size length
	static final byte X_RECV = 0x42; // receive fixed size length
	static final byte X_RECV_VAR = 0x43; // receive first byte for length of
											// rest
											// to receive
	static final byte X_RECV_VAR_MIN_1 = 0x44; // receive first byte for length
												// of
												// rest
												// to receive
	static final byte X_RESET_BOOTLOADER = 0x45;
	static final byte X_RESET_RUN = 0x46;

	public static final byte GET = 0;
	public static final byte GET_VERSION = 1;
	public static final byte GET_ID = 2;
	public static final byte READ_MEMORY = 0x11;
	public static final byte GO = 0x21;
	public static final byte WRITE_MEMORY = 0x31;
	public static final byte ERASE_MEMORY = 0x41;
	public static final byte EXTENDED_ERASE_MEMORY = 0x44;
	public static final byte WRITE_PROTECT = 0x63;
	public static final byte WRITE_UNPROTECT = 0x73;
	public static final byte READ_PROTECT = (byte) 0x82;
	public static final byte READ_UNPROTECT = (byte) 0x92;

	static final byte ACK = (byte) 0x79;
	static final byte NACK = (byte) 0x1F;

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 3];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 3] = hexArray[v >>> 4];
			hexChars[j * 3 + 1] = hexArray[v & 0x0F];
			hexChars[j * 3 + 2] = ' ';
		}
		return new String(hexChars);
	}

	public byte version;
	public byte commands[] = null;
	public byte pid[] = null;
	public byte memory[] = null;

	static void ASSERT(boolean expr) throws ArrayIndexOutOfBoundsException {
		if (expr)
			return;
		throw new ArrayIndexOutOfBoundsException(" ASSERTION FAILED ");
	}

	static byte xor(int a) {
		return (byte) (a ^ (-1) & 0xFF);
	}

	static byte slice(int word, int offset) {
		return (byte) ((word >> (offset * 8)) & 0xFF);
	}

	static byte fullXor(int word) {
		return (byte) (slice(word, 0) ^ slice(word, 1) ^ slice(word, 2) ^ slice(word, 3));
	}

	static byte fullXor(byte[] arr) {
		byte x = arr[0];
		for (int i = 1; i < arr.length; i++)
			x ^= arr[i];
		return x;
	}

	static void add(byte[] arr, int offset, int value) {
		arr[offset] = slice(value, 1);
		arr[offset + 1] = slice(value, 0);
	}

	static byte crc(byte[] arr, int offset, int length) {
		byte x = arr[offset];
		for (int i = 1; i < length; i++)
			x ^= arr[offset + i];
		return x;
	}

	static class Get {
		@Getter
		static byte version;
		@Getter
		static byte[] commands;

		static byte[] request() {
			return new byte[] { X_SEND, 1, GET, xor(GET), X_WAIT_ACK, X_RECV_VAR, X_WAIT_ACK };
		}

		static boolean parse(Bytes reply) {
			HashMap<String, Object> resp;
			if (reply.read() != ACK)
				return false;
			int length = (reply.read() & 0xFF) + 1;
			if (reply.available() < length + 1)
				return false;
			version = reply.read();
			commands = new byte[length];
			for (int i = 0; i < length; i++)
				commands[i] = reply.read();
			if (reply.read() != ACK)
				return false;
			return true;

		}

	}

	static class GetVersion {
		@Getter
		byte version;

		byte[] request() {
			return new byte[] { X_SEND, 1, GET_VERSION, xor(GET_VERSION), X_WAIT_ACK, X_RECV, 3, X_WAIT_ACK };
		}

		boolean parse(Bytes reply) {
			if (reply.available() < 5)
				return false;
			if (reply.read() != ACK)
				return false;
			version = reply.read();
			reply.read();
			reply.read();
			if (reply.read() != ACK)
				return false;
			return true;
		}
	}

	static class GetId {
		byte[] pid;

		static byte[] request() {
			return new byte[] { X_SEND, 1, GET_ID, xor(GET_ID), X_WAIT_ACK, X_RECV_VAR, X_RECV, 2, X_WAIT_ACK };
		}

		boolean parse(Bytes reply) {
			if (reply.read() != ACK)
				return false;
			int length = (reply.read() & 0xFF) + 1;
			if (reply.available() < length + 1)
				return false;
			pid = new byte[length];
			for (int i = 0; i < length; i++)
				pid[i] = reply.read();
			if (reply.read() != ACK)
				return false;
			return true;
		}
	}

	static class ReadMemory {
		byte[] memory;

		static byte[] request(int address, int length) {
			ASSERT(length > 0 && length < 257);
			byte Read_Memory[] = { X_SEND, 1, READ_MEMORY, xor(READ_MEMORY), X_WAIT_ACK, //
					X_SEND, 4, slice(address, 3), slice(address, 2), //
					slice(address, 1), slice(address, 0), fullXor(address), X_WAIT_ACK, //
					X_SEND, 1, (byte) (length - 1), xor(length - 1), X_WAIT_ACK, //
					X_RECV, (byte) (length - 1), X_WAIT_ACK };
			return Read_Memory;
		}

		boolean parse(Bytes reply) {
			if (reply.read() != ACK)
				return false;
			int length = (reply.read() & 0xFF) + 1;
			if (reply.available() < length + 1)
				return false;
			memory = new byte[length];
			for (int i = 0; i < length; i++)
				memory[i] = reply.read();
			byte xr = reply.read(); // TODO check CRC
			if (reply.read() != ACK)
				return false;
			return true;
		}
	}

	static class Go {

		static byte[] request(int address) {
			return new byte[] { X_SEND, 1, GO, xor(GO), X_WAIT_ACK, X_SEND, 5, slice(address, 3), slice(address, 2), //
					slice(address, 1), slice(address, 0), fullXor(address), X_WAIT_ACK };
		}

		boolean parse(Bytes bytes) {
			return true;
		}
	}

	static class WriteMemory {
		static byte[] request(int address, byte[] instr) {
			byte[] Write_Memory = { X_SEND, 1, WRITE_MEMORY, xor(WRITE_MEMORY), X_WAIT_ACK, X_SEND, 5,
					slice(address, 3), slice(address, 2), //
					slice(address, 1), slice(address, 0), fullXor(address), X_WAIT_ACK };
			byte[] Write_Memory_Closure = { fullXor(instr), X_WAIT_ACK };
			byte[] result = new byte[instr.length + Write_Memory.length + Write_Memory_Closure.length];
			System.arraycopy(Write_Memory, 0, result, 0, Write_Memory.length);
			System.arraycopy(instr, 0, result, Write_Memory.length, instr.length);
			System.arraycopy(Write_Memory_Closure, 0, result, Write_Memory.length + instr.length,
					Write_Memory_Closure.length);
			return result;
		}
		boolean parse(Bytes bytes) {
			return true;
		}
	}

	static class GlobalEraseMemory {
		static byte[] request() {
			byte[] Global_Erase_Memory = { X_SEND, 1, ERASE_MEMORY, xor(ERASE_MEMORY), X_WAIT_ACK, 0, xor(0),
					X_WAIT_ACK };
			return Global_Erase_Memory;
		}
		boolean parse(Bytes bytes) {
			return true;
		}
	}

	static class EraseMemory {
		static byte[] request(byte[] pages) {
			byte[] Erase_Memory = { X_SEND, 1, ERASE_MEMORY, xor(ERASE_MEMORY), X_WAIT_ACK, (byte) (pages.length - 1) };
			byte[] result = new byte[Erase_Memory.length + pages.length];
			System.arraycopy(Erase_Memory, 0, result, 0, Erase_Memory.length);
			System.arraycopy(pages, 0, result, Erase_Memory.length, pages.length);
			// TODO add length in checksum
			result[Erase_Memory.length + pages.length] = fullXor(pages);
			result[Erase_Memory.length + pages.length + 1] = X_WAIT_ACK;
			return result;
		}
		boolean parse(Bytes bytes) {
			return true;
		}
	}

	static class ExtendedEraseMemory {

		static byte[] request(int[] pages) {
			byte[] Extended_Erase_Memory = { X_SEND, 1, EXTENDED_ERASE_MEMORY, xor(EXTENDED_ERASE_MEMORY), X_WAIT_ACK,
					X_SEND, (byte) (pages.length - 1) };
			byte[] result = new byte[Extended_Erase_Memory.length + pages.length * 2 + 4];
			System.arraycopy(Extended_Erase_Memory, 0, result, 0, Extended_Erase_Memory.length);
			int offset = Extended_Erase_Memory.length;
			byte crc;
			crc = result[offset++] = slice(pages.length - 1, 1);
			crc ^= result[offset++] = slice(pages.length - 1, 0);
			for (int i = 0; i < pages.length; i++) {
				crc ^= result[offset++] = slice(pages[i], 1);
				crc ^= result[offset++] = slice(pages[i], 0);
			}
			result[offset++] = crc;
			result[offset++] = X_WAIT_ACK;
			return result;
		}
		boolean parse(Bytes bytes) {
			return true;
		}
	}

	static class WriteProtect {

		static byte[] request(byte[] sectors) {
			byte[] WriteProtect = { X_SEND, 1, WRITE_PROTECT, xor(WRITE_PROTECT), X_WAIT_ACK, X_SEND,
					(byte) (sectors.length - 1) };
			byte crc = (byte) (sectors.length - 1);
			byte[] result = new byte[WriteProtect.length + sectors.length * 2 + 4];
			System.arraycopy(WriteProtect, 0, result, 0, WriteProtect.length);
			int offset = WriteProtect.length;
			for (int i = 0; i < sectors.length; i++) {
				crc ^= result[offset++] = sectors[i];
			}
			result[offset++] = crc;
			result[offset++] = X_WAIT_ACK;
			return result;
		}
		boolean parse(Bytes bytes) {
			return true;
		}
	}

	static class WriteUnprotect {
		static byte[] request() {
			byte[] WriteUnprotect = { X_SEND, 1, WRITE_UNPROTECT, xor(WRITE_UNPROTECT), X_WAIT_ACK, X_WAIT_ACK };
			return WriteUnprotect;
		}
		boolean parse(Bytes bytes) {
			return true;
		}
	}

	static class ReadProtect {
		static byte[] request() {
			byte[] ReadProtect = { X_SEND, 1, READ_PROTECT, xor(READ_PROTECT), X_WAIT_ACK, X_WAIT_ACK };
			return ReadProtect;
		}
		boolean parse(Bytes bytes) {
			return true;
		}
	}

	static class ReadUnprotect {

		static byte[] request() {
			byte[] ReadUnprotect = { X_SEND, 1, READ_UNPROTECT, xor(READ_UNPROTECT), X_WAIT_ACK, X_WAIT_ACK };
			return ReadUnprotect;
		}
		boolean parse(Bytes bytes) {
			return true;
		}
	}

	static int globalId = 1;

	static int newId() {
		return globalId++ & 0xFFFF;
	}

	public static void main(String[] args) {
		System.out.println(bytesToHex(Bootloader.Get.request()));
		System.out.println(bytesToHex(ReadMemory.request(0xFF00FF00, 256)));
		System.out.println(bytesToHex(WriteMemory.request(0xA1A2A3A4, new byte[] { 1, 2, 3, 4, 5, 7, 11, 13, 15 })));
		System.out.println(bytesToHex(ExtendedEraseMemory.request(new int[] { 1, 2 })));
	}

}

package be.limero.programmer;

import java.util.logging.Logger;

import be.limero.programmer.commands.Stm32Msg;

/*
 * To keep track of outstanding commands to STM32 bootloader
 * 
 */

public class Stm32Cmd {
	public enum Status {
		NEW, SEND, RECEIVED, DELETE, FAILED
	};

	private static final Logger log = Logger.getLogger(Stm32Cmd.class.getName());
	public static final int STM32_RESET = 0xFF;
	public static final int STM32_GET_VERSION_AND_COMMANDS = 0x00;
	public static final int STM32_GET_VERSION_AND_READ_PROTECTION = 0X01;
	public static final int STM32_GET_ID = 0x02;
	public static final int STM32_READ_MEMORY = 0x11;
	public static final int STM32_GO = 0x21;
	public static final int STM32_WRITE_MEMORY = 0x31;
	public static final int STM32_ERASE = 0x43;
	public static final int STM32_ERASE_EXTENDED = 0x44;
	public static final int STM32_WRITE_PROTECT = 0x63;
	public static final int STM32_WRITE_UNPROTECT = 0x73;
	public static final int STM32_READOUT_PROTECT = 0x82;
	public static final int STM32_READOUT_UNPROTECT = 0x92;
	public static final int STM32_SYNC = 0x7F;
	public static final int STM32_ACK = 0x79;
	public static final int STM32_NACK = 0x1F;

	Status status;
	Stm32Msg request;
	Stm32Msg response;

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Stm32Msg getRequest() {
		return request;
	}

	public void setRequest(Stm32Msg request) {
		this.request = request;
	}

	public Stm32Msg getResponse() {
		return response;
	}

	public void setResponse(Stm32Msg response) {
		this.response = response;
	}

	public int getId() {
		return request.messageId;
	}

	public void setId(int id) {
		request.messageId = id;
		log.warning("sure about this ?");
	}

	// ___________________________________________________________________________________

}

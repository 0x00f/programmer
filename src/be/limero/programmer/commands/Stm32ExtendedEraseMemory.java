package be.limero.programmer.commands;

import java.util.logging.Logger;

import be.limero.common.Bytes;
import be.limero.programmer.Stm32Model;

public class Stm32ExtendedEraseMemory extends Stm32Msg {

	private static final Logger log = Logger.getLogger(Stm32GetVersionCommands.class.getName());

	static byte getCmdByte() {
		return (byte)0x44;
	}
	
	/*
	 * see AN3155 for details on page selections
	 */

	public Stm32ExtendedEraseMemory() {
		super(100);
		cmd = Stm32Msg.CMD.STM32_CMD_BOOT_REQ.ordinal();
		messageId = Stm32Msg.getNextId();
		data.add(crcBytes(getCmdByte()));
		acks.add(2);
		build();
	};

	public void handle(Stm32Model stm32) {
		parse();
		Bytes bytes;
		// ACK : 0:CMD ACK, 1:ADDRESS_ACK, 2:LENGTH:ACK, 3:DATA
		if ((bytes = data.get(0)) != null) {
			// first 3 bytes == ACKS
			// TODO update memoryImage
		}
		log.warning(" handle failed ");
	}
}

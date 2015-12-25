package be.limero.programmer.commands;

import java.util.logging.Logger;

import be.limero.common.Bytes;
import be.limero.programmer.Stm32Model;

public class Stm32EnterBootloader extends Stm32Msg {


	private static final Logger log = Logger.getLogger(Stm32GetVersionCommands.class.getName());

	static byte getCmdByte() {
		return 0x00; // not used for reset 
	}
	
	public Stm32EnterBootloader() {
		super(100);
		cmd = Stm32Msg.CMD.STM32_CMD_BOOT_ENTER.ordinal();
		messageId = Stm32Msg.getNextId();
		build();
	};

	void handle(Stm32Model stm32) {
		parse();
		Bytes bytes;
		// ACK : 0:CMD ACK, 1:ADDRESS_ACK, 2:LENGTH:ACK, 3:DATA
		if ((bytes = data.get(0)) != null) {
			// first 3 bytes == ACKS
			//TODO update memoryImage
		}
		log.warning(" handle failed ");
	}
}

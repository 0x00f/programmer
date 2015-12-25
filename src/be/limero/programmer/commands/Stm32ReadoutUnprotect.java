package be.limero.programmer.commands;

import java.util.logging.Logger;

import be.limero.common.Bytes;
import be.limero.programmer.Stm32Model;

public class Stm32ReadoutUnprotect extends Stm32Msg {


	private static final Logger log = Logger.getLogger(Stm32GetVersionCommands.class.getName());

	static byte getCmdByte() {
		return 0x11;
	}
	
	public Stm32ReadoutUnprotect(int address,int length) {
		super(100);
		cmd = Stm32Msg.CMD.STM32_CMD_BOOT_REQ.ordinal();
		messageId = Stm32Msg.getNextId();
		data.add(crcBytes(getCmdByte()));
		acks.add(1);
		data.add(addCrc(quadToBytes(address)));
		acks.add(1);
		data.add(crcBytes((byte)(length-1))); // request length -1 
		acks.add(1);
		data.add(new Bytes(0)); 		// empty  
		acks.add(-(length+1));			// wait for bytes  + CRC		
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

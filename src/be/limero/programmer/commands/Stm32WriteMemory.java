package be.limero.programmer.commands;

import java.util.logging.Logger;

import be.limero.common.Bytes;
import be.limero.programmer.Stm32Model;

public class Stm32WriteMemory extends Stm32Msg {


	private static final Logger log = Logger.getLogger(Stm32GetVersionCommands.class.getName());

	static byte getCmdByte() {
		return 0x31;
	}
	


	public Stm32WriteMemory(int address,Bytes mem) {
		super(100);
		cmd = Stm32Msg.CMD.STM32_CMD_BOOT_REQ.ordinal();
		messageId = Stm32Msg.getNextId();
		data.add(complementByte(getCmdByte()));
		acks.add(1);
		data.add(addCrc(quadToBytes(address)));
		acks.add(1);
		//TODO add length 
		Bytes bytes=new Bytes(300);
		bytes.write((byte)(mem.length()-1));
		offset(0);
		for(int i=0;i<mem.length();i++)
			bytes.write(mem.read());
		data.add(addCrc(bytes)); // request length -1 
		acks.add(1);	
		build();
	};

	public void handle(Stm32Model stm32) {
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

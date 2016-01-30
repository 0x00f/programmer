package be.limero.programmer.commands;

import java.util.logging.Level;
import java.util.logging.Logger;

import be.limero.common.Bytes;
import be.limero.programmer.Stm32Model;

public class Stm32ReadMemory extends Stm32Msg {

	private static final Logger log = Logger.getLogger(Stm32GetVersionCommands.class.getName());

	static byte getCmdByte() {
		return 0x11;
	}

	public Stm32ReadMemory(int address, int length) {
		super(100);
		cmd = Stm32Msg.CMD.STM32_CMD_BOOT_REQ.ordinal();
		messageId = Stm32Msg.getNextId();
		data.add(complementByte(getCmdByte()));
		acks.add(1);
		data.add(addCrc(quadToBytes(address)));
		acks.add(1);
		data.add(complementByte((byte) (length - 1))); // request length -1
		acks.add(1);
		data.add(new Bytes(0)); // empty
//		acks.add(1);
		acks.add(-(length + 1+3)); // wait for bytes + CRC + 3 ACKS
		build();
	};

	public void handle(Stm32Model stm32) {
		try {
			parse(); // 79 01 04 10 79
			if (error == 0) {
				Bytes bytes = data.get(0);
				stm32.setId(bytes.peek(2) * 256 + bytes.peek(3));
			} else {
				log.log(Level.WARNING, " handle failed error : " + error);
			}
		} catch (Exception e) {
			log.log(Level.WARNING, " handle failed ", e);
		}
	}
}

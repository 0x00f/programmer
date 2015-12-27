package be.limero.programmer.commands;

import java.util.logging.Level;
import java.util.logging.Logger;

import be.limero.common.Bytes;
import be.limero.programmer.Stm32Model;

public class Stm32GetVersionCommands extends Stm32Msg {

	private static final Logger log = Logger.getLogger(Stm32GetVersionCommands.class.getName());

	static byte getCmdByte() {
		return 0x00;
	}

	public Stm32GetVersionCommands() {
		super(100);
		cmd = Stm32Msg.CMD.STM32_CMD_BOOT_REQ.ordinal();
		messageId = Stm32Msg.getNextId();
		data.add(complementByte(getCmdByte()));
		acks.add(2);
		build();
	};

	public void handle(Stm32Model stm32) {// 0x79 0B 22 00 01 02 11 21 31 43 63 73 82 92 79
		try {
			parse();
			Bytes bytes = data.get(0);
			stm32.setBootloaderVersion(bytes.peek(2));
			int length=bytes.peek(1);
			Bytes commands=bytes.sub(3, length);
			stm32.setCommands(commands.bytes());
		} catch (Exception e) {
			log.log(Level.WARNING, " handle failed ", e);
		}

	}
}

package be.limero.programmer.commands;

import java.util.logging.Logger;

import be.limero.common.Bytes;
import be.limero.programmer.Stm32Model;

public class Stm32GetVersionCommands extends Stm32Msg {

	private static final Logger log = Logger.getLogger(Stm32GetVersionCommands.class.getName());

	static byte getCmdByte() {
		return 0x02;
	}

	public Stm32GetVersionCommands() {
		super(100);
		cmd = Stm32Msg.CMD.STM32_CMD_BOOT_REQ.ordinal();
		messageId = Stm32Msg.getNextId();
		data.add(crcBytes(getCmdByte()));
		acks.add(2);
		build();
	};

	void handle(Stm32Model stm32) {
		parse();
		Bytes bytes;
		if ((bytes = data.get(0)) != null) {
			if (bytes.peek(0) == 0x79 && bytes.length() > 3) {
				stm32.setBootloaderVersion(bytes.peek(1));
				return;
			}
		}
		log.warning(" handle failed ");
	}
}

package be.limero.programmer.commands;

import java.util.logging.Logger;

import be.limero.common.Bytes;
import be.limero.programmer.Stm32Model;

public class Stm32GetVersionReadProtection extends Stm32Msg {

	private static final Logger log = Logger.getLogger(Stm32GetVersionReadProtection.class.getName());

	static byte getCmdByte() {
		return 0x01;
	}

	public Stm32GetVersionReadProtection() {
		super(100);
		cmd = Stm32Msg.CMD.STM32_CMD_BOOT_REQ.ordinal();
		messageId = Stm32Msg.getNextId();
		data.add(complementByte(getCmdByte()));
		acks.add(2);
		build();
	};

	public void handle(Stm32Model stm32) {
		parse();
		Bytes bytes;
		if ((bytes = data.get(0)) != null) {

		} else
			log.warning(" handle failed ");
	}
}

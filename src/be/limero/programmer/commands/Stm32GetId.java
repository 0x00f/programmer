package be.limero.programmer.commands;

import java.util.logging.Level;
import java.util.logging.Logger;

import be.limero.common.Bytes;
import be.limero.programmer.Stm32Model;

public class Stm32GetId extends Stm32Msg {

	private static final Logger log = Logger.getLogger(Stm32GetId.class.getName());

	static byte getCmdByte() {
		return 0x02;
	}

	public Stm32GetId() {
		super(100);
		cmd = Stm32Msg.CMD.STM32_CMD_BOOT_REQ.ordinal();
		messageId = Stm32Msg.getNextId();
		data.add(complementByte(getCmdByte()));
		acks.add(2);
		build();
	};

	public void handle(Stm32Model stm32) {
		try {
			parse();		// 79 01 04 10 79
			if (error == 0 ) {
				Bytes bytes = data.get(0);
				stm32.setId(bytes.peek(2)*256+bytes.peek(3));
			}
		} catch (Exception e) {
			log.log(Level.WARNING, " handle failed ", e);
		}
	}
}

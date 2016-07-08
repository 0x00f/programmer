package be.limero.programmer;

import java.net.InetSocketAddress;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import be.limero.network.Request;
import be.limero.network.UdpPipe;
import be.limero.programmer.ui.Stm32Programmer;
import be.limero.util.Cbor;
import be.limero.util.Slip;

public class Stm32Controller extends UntypedActor {
	LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	ActorRef proxy;
	Slip slip;

	boolean _tcpConnected = false;

	// start Mqtt exchange thread
	Stm32Model model;
	Stm32Programmer ui;
	InetSocketAddress remote = new InetSocketAddress("192.168.0.131", 23);

	public Stm32Controller(Stm32Programmer frame, Stm32Model model) {

		this.model = model;
		slip = new Slip(1024);
		ui = frame;
		ui.updateView();
		proxy = ActorSystem.create("System").actorOf(Props.create(UdpPipe.class), "Proxy-UdpPipe");
		/*
		 * tcp = ActorSystem.create("MySystem")
		 * .actorOf(Props.create(TcpClient.class));
		 */
	}

	public void getModelFromView() {
		model.setHost(ui.getTxtMqttConnection().getText());
	}

	public void connect() {
		remote = new InetSocketAddress(model.getHost(), model.getPort());
		proxy.tell(remote, getSelf());
	}

	public void disconnect() {
		// tcp.tell(TcpMessage.confirmedClose(), getSelf());
	}

	public void reset() {
		proxy.tell(new Request(Request.Cmd.RESET, new byte[] {}).toCbor(), getSelf());
	}

	public void sendCommand(byte[] msg) {
		proxy.tell(new Request(Request.Cmd.EXEC, msg).toCbor(), self());
	}

	public Stm32Model getModel() {
		return model;
	}

	public void setModel(Stm32Model model) {
		this.model = model;
	}

	public Stm32Programmer getUi() {
		return ui;
	}

	public void setUi(Stm32Programmer ui) {
		this.ui = ui;
	}

	@Override
	public void onReceive(Object msg) throws Exception {
		log.info(" class = " + msg.getClass().getName() + "  msg = " + msg + " sender : " + sender());
		if (msg instanceof Cbor) {

			/*
			 * Tcp.Received rcv = (Tcp.Received) msg; byte[] arr =
			 * rcv.data().toArray(); Bytes bytes = new Bytes(arr); while
			 * (bytes.hasData()) { if (slip.fill(bytes.read())) { if
			 * (Slip.isGoodCrc(slip)) { Slip.removeCrc(slip); slip.offset(0);
			 * Cbor cbor = new Cbor(1025); while (slip.hasData()) {
			 * cbor.write(slip.read()); } log.info("cbor =" + cbor.toString());
			 * cbor.offset(0); int cmd = cbor.getInteger(); if (cmd ==
			 * Request.Cmd.LOG_OUTPUT.ordinal()) { cbor.getInteger();
			 * cbor.getInteger(); Bytes byt = cbor.getBytes(); String line = new
			 * String(byt.bytes(), "UTF-8"); log.info(" log " + line);
			 * model.setLog(model.getLog() + "\n" + line); } } slip.reset(); }
			 */
		} else if (msg instanceof String) {

			switch ((String) msg) {
			case "connect": {
				connect();
				model.setConnected(true);
				break;
			}
			case "disconnect": {
				disconnect();
				model.setConnected(false);
				break;
			}
			case "reset": {
				reset();
				break;
			}
			case "go": {
				sendCommand(Stm32Protocol.Go(0x8000000));
				break;
			}
			case "get": {
				sendCommand(Stm32Protocol.Get());
				break;
			}
			case "getId": {
				sendCommand(Stm32Protocol.GetId());
				break;
			}
			case "getVersion": {
				sendCommand(Stm32Protocol.GetVersion());
				break;
			}
			case "globalErase": {
				sendCommand(Stm32Protocol.GlobalEraseMemory());
				break;
			}
			case "eraseMemory": {
				sendCommand(Stm32Protocol.EraseMemory(new byte[2]));
				break;
			}
			case "extendedEraseMemory": {
				sendCommand(Stm32Protocol.ExtendedEraseMemory(new int[2]));
				break;
			}
			case "writeProtect": {
				sendCommand(Stm32Protocol.WriteProtect(new byte[2]));
				break;
			}
			case "writeUnProtect": {
				sendCommand(Stm32Protocol.WriteUnprotect());
				break;
			}
			case "readProtect": {
				sendCommand(Stm32Protocol.ReadProtect());
				break;
			}
			case "readUnProtect": {
				sendCommand(Stm32Protocol.ReadUnprotect());
				break;
			}
			case "read": {
				int start = 0x8000000;
				sendCommand(Stm32Protocol.ReadMemory(start, 256));
				sendCommand(Stm32Protocol.ReadMemory(start + 256, 256));
				break;
			}
			case "program": {
				int start = 0x8000000;
				byte[] data = new byte[256];
				for (int i = 0; i < 256; i++)
					data[i] = (byte) (255 - i);
				sendCommand(Stm32Protocol.WriteMemory(start, data));
				break;
			}

			}
		}
		ui.updateView();
	}

}

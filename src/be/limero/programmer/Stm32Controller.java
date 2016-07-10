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
import be.limero.util.Bytes;
import be.limero.util.Cbor;
import be.limero.util.Slip;

public class Stm32Controller extends UntypedActor {
	LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	ActorRef proxy;
	Slip slip;
	Stm32Protocol stm32Protocol = new Stm32Protocol();

	enum Route {
		RESET, EXEC_GET, EXEC_GET_ID, EXEC_GET_VERSION, EXEC_READ_MEMORY, EXEC_WRITE_MEMORY, //
		EXEC_GO, EXEC_ERASE_MEMORY, EXEC_EXTENDED_ERASE_MEMORY, EXEC_WRITE_PROTECT, //
		EXEC_WRITE_UNPROTECT, EXEC_READ_PROTECT, EXEC_READ_UNPROTECT
	};

	boolean _tcpConnected = false;

	// start Mqtt exchange thread
	Stm32Model model;
	Stm32Programmer ui;
	InetSocketAddress remote = new InetSocketAddress("192.168.0.131", 23);
	Bytes binFile;

	public Stm32Controller(Stm32Programmer frame, Stm32Model model) {

		this.model = model;
		slip = new Slip(1024);
		ui = frame;
		ui.updateView();
		proxy = ActorSystem.create("System")
				.actorOf(Props.create(UdpPipe.class), "Proxy-UdpPipe");
		binFile = new Bytes(256000);
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
		proxy.tell(new Request(Request.Cmd.RESET, new byte[] {}).toCbor(),
				getSelf());
	}

	public void sendCommand(Cmd id,Request.Cmd cmd,byte[] msg) {
		Request req = new Request(cmd,id.ordinal(), msg);
		proxy.tell(req.toCbor(), self());
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
		log.info(" class = " + msg.getClass().getName() + "  msg = " + msg
				+ " sender : " + sender());
		if (msg instanceof Cbor) {
			Cbor cbor = (Cbor) msg;
			onResponse(cbor);

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
				sendCommand(Cmd.EXEC_GO,Request.Cmd.EXEC,Stm32Protocol.Go(0x8000000));
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
				break;
			}
			case "program": {
				int start = 0x8000000;
				byte[] data = new byte[256];
				for (int i = 0; i < 256; i++)
					data[i] = binFile.read();
				sendCommand(Stm32Protocol.WriteMemory(start, data));
				break;
			}

			}
		}
		ui.updateView();
	}

	void onResponse(Cbor cbor) {
		cbor.offset(0);
		int cmd = cbor.getInteger();
		int id = cbor.getInteger();
		int error = cbor.getInteger();
		Bytes bytes = cbor.getBytes();
		log.info(" reply : cmd=" + cmd + " id=" + id + " error=" + error
				+ " bytes=" + bytes.toHex());

	}

}

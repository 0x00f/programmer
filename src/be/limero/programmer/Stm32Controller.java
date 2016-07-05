package be.limero.programmer;

import java.net.InetSocketAddress;

import javax.swing.SwingUtilities;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Tcp;
import akka.io.TcpMessage;
import akka.util.ByteString;
import akka.util.ByteStringBuilder;
import be.limero.network.Request;
import be.limero.programmer.ui.Stm32Programmer;
import be.limero.util.Bytes;
import be.limero.util.Cbor;
import be.limero.util.Slip;
import be.limero.util.Str;

public class Stm32Controller extends UntypedActor {
	LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	
	final ActorRef tcpManager = Tcp.get(getContext().system()).manager();
	ActorRef tcp;
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
		tcpManager.tell(TcpMessage.connect(remote), getSelf());
	}

	public void disconnect() {
		tcp.tell(TcpMessage.confirmedClose(), getSelf());
	}

	public void reset() {
		Bytes bytes = new Request(Request.Cmd.RESET, new byte[] {}).toSlip();
//		Bytes bytes = Slip.encode(Slip.addCrc(cbor));
		ByteString bs = new ByteStringBuilder().putBytes(bytes.bytes()).result();
		tcp.tell(TcpMessage.write(bs), self());
	}

	public void go() {
		Str str=new Str("GET /wiki/Hoofdpagina HTTP/1.1\n\n\n");
		ByteString bs = new ByteStringBuilder().putBytes(str.bytes()).result();
		tcp.tell(TcpMessage.write(bs), self());
	}

	public void getId() {
		sendCommand(Stm32Protocol.GetId());
	}

	public void getVersionCommands() {
		sendCommand(Stm32Protocol.GetVersion());
	}

	public void sendCommand(byte[] msg) {
		Bytes bytes = new Request(Request.Cmd.EXEC, msg).toSlip();
		ByteString bs = new ByteStringBuilder().putBytes(bytes.bytes()).result();
		tcp.tell(TcpMessage.write(bs), self());
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
		log.info(" msg = " + msg + " sender : " + sender() + " tcp: " + tcp);
		if (msg instanceof Tcp.Connected) {
			
            getSender().tell(TcpMessage.register(getSelf()), getSelf());
			_tcpConnected = true;
			model.setConnected(true);
			tcp = sender();
		} else if (msg instanceof Tcp.ConnectionClosed) {
			
			_tcpConnected = false;
			model.setConnected(false);
			
		} else if (msg instanceof Tcp.CommandFailed) {
			
			_tcpConnected = false;
			model.setConnected(false);
			
		}else if (msg instanceof Tcp.Received) {
			
			Tcp.Received rcv= (Tcp.Received) msg;
			byte[] arr = rcv.data().toArray();
			Bytes bytes = new Bytes(arr);
			while(bytes.hasData()) {
				if (slip.fill(bytes.read())) {
					if ( Slip.isGoodCrc(slip)) {
						Slip.removeCrc(slip);
						slip.offset(0);
						Cbor cbor = new Cbor(1025);
						while (slip.hasData()) {
							cbor.write(slip.read());
						}
						log.info("cbor =" + cbor.toString());
						cbor.offset(0);
						int cmd = cbor.getInteger();
						if (cmd == Request.Cmd.LOG_OUTPUT.ordinal()) {
							cbor.getInteger();
							cbor.getInteger();
							Bytes byt = cbor.getBytes();
							String line = new String(byt.bytes(),
									"UTF-8");
							log.info(" log " + line);
							model.setLog(model.getLog()+"\n"+line);
						}
					}
					slip.reset();
				}
			}
			
		} else if (msg instanceof String) {
					
			switch ((String) msg) {
			case "connect": {
				connect();
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
				go();
				break;
			}
			}
		}
		ui.updateView();
	}

}

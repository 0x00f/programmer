package be.limero.network;

import java.net.InetSocketAddress;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.io.Udp;
import akka.io.UdpMessage;
import akka.util.ByteString;
import be.limero.util.Cbor;

public class UdpPipe extends UntypedActor {
	InetSocketAddress remote;
	ActorRef nextActor;
	ActorRef udpSender;
	ActorRef udpReceiver;
	LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public UdpPipe() {
		remote = new InetSocketAddress("localhost", 3881);
		this.remote = null;

		// request creation of a SimpleSender
		final ActorRef mgr = Udp.get(getContext().system()).getManager();

		mgr.tell(UdpMessage.bind(getSelf(), new InetSocketAddress("localhost", 3881)), getSelf());
		mgr.tell(UdpMessage.simpleSender(), getSelf());
	}

	@Override
	public void onReceive(Object msg) {
		log.info(" class = " + msg.getClass().getName() + "  msg = " + msg + " sender : " + sender());

		if (msg instanceof Udp.SimpleSenderReady) {
			udpSender = sender();
			// getContext().become(ready(getSender()));
		} else if (msg instanceof Udp.Bound) {
			final Udp.Bound b = (Udp.Bound) msg;
			udpReceiver = sender();
			// getContext().become(ready(getSender()));
		} else if (msg instanceof Cbor) { // from Stm32Controller
			
			Cbor cbor = (Cbor) msg;
			nextActor=sender();
			log.info(" sending Cbor : " + cbor.toString() + " to " + remote);
			udpSender.tell(UdpMessage.send(ByteString.fromArray(cbor.bytes()), remote), getSelf());
			
		} else if (msg instanceof Udp.Received) {
			
			final Udp.Received r = (Udp.Received) msg;
			Cbor cbor = new Cbor(r.data().toArray());
			log.info(" receiving Cbor : " + cbor.toString() + " from " + r.sender() );
			nextActor.tell(cbor, getSelf());

		} else if (msg.equals(UdpMessage.unbind())) {
			sender().tell(msg, getSelf());

		} else if (msg instanceof Udp.Unbound) {
			getContext().stop(getSelf());

		} else if (msg instanceof InetSocketAddress) {
			remote = (InetSocketAddress) msg;
			log.info(" remote : " + remote);

		} else
			unhandled(msg);

	}

}
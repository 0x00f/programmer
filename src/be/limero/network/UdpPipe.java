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
	InetSocketAddress local;
	ActorRef nextActor;
	ActorRef udpSender;
	ActorRef udpReceiver;
	LoggingAdapter log = Logging.getLogger(getContext().system(), this);
	final ActorRef mgr;

	public UdpPipe() {
		this.remote = null;

		// request creation of a SimpleSender
		mgr = Udp.get(getContext().system()).getManager();
		local = new InetSocketAddress(3881);

		//		mgr.tell(UdpMessage.bind(getSelf(),
		//				new InetSocketAddress("localhost", 3881)), getSelf());
		//		mgr.tell(UdpMessage.simpleSender(), getSelf());
	}

	@Override
	public void onReceive(Object msg) {
		log.info(" class = " + msg.getClass().getName() + "  msg = " + msg
				+ " sender : " + sender());

		if (msg instanceof Udp.SimpleSenderReady) {
			udpSender = sender();
			Udp.SimpleSenderReady ssr = (Udp.SimpleSenderReady) msg;
			// getContext().become(ready(getSender()));
		} else if (msg instanceof Udp.Bound) {
			final Udp.Bound bound = (Udp.Bound) msg;
			local = bound.localAddress();
			udpReceiver = sender();
			// getContext().become(ready(getSender()));
		} else if (msg instanceof Cbor) { // from Stm32Controller

			Cbor cbor = (Cbor) msg;
			nextActor = sender();
			log.info(" sending Cbor : " + cbor.toString() + " to " + remote);
			udpReceiver.tell(
					UdpMessage.send(ByteString.fromArray(cbor.bytes()), remote),
					getSelf());

		} else if (msg instanceof Udp.Received) {

			final Udp.Received r = (Udp.Received) msg;
			Cbor cbor = new Cbor(r.data().toArray());
			log.info(" receiving Cbor : " + cbor.toString() + " from "
					+ r.sender());
			nextActor.tell(cbor, getSelf());

		} else if (msg.equals(UdpMessage.unbind())) {
			sender().tell(msg, getSelf());

		} else if (msg instanceof Udp.Unbound) {
			getContext().stop(getSelf());

		} else if (msg instanceof InetSocketAddress) {
			remote = (InetSocketAddress) msg;
			log.info(" remote : " + remote);
			mgr.tell(UdpMessage.bind(getSelf(), local), getSelf());

		} else
			unhandled(msg);

	}

}
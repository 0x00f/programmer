package be.limero.vertx;

import be.limero.util.Cbor;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class UdpVerticle extends AbstractVerticle {
	private final static Logger log = LoggerFactory.getLogger(UdpVerticle.class);
	DatagramSocket socket;
	EventBus eb;
	/*
	 * (non-Javadoc)
	 * 
	 * @see io.vertx.core.AbstractVerticle#start()
	 */
	@Override
	public void start() throws Exception {
		 eb = vertx.eventBus();

		super.start();
		socket = vertx.createDatagramSocket(new DatagramSocketOptions());
		socket.listen(3881, "0.0.0.0", asyncResult -> {
			if (asyncResult.succeeded()) {
				socket.handler(packet -> {
					eb.send("controller",packet.data());
					log.info("Msg received "+packet.toString());
				});
			} else {
				log.info("Listen failed" + asyncResult.cause());
			}
		});
		Buffer buffer = Buffer.buffer("content");
		// Send a Buffer
		socket.send(buffer, 3881, "192.168.0.132", asyncResult -> {
			log.info("Send succeeded? " + asyncResult.succeeded());
		});


		eb.consumer("udp", message -> {
			if ( message.body() instanceof JsonObject ) {
				log.info(" received json ");
			};
			log.info("I have received a EB message: " + message.body() );
			message.address();
		});

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.vertx.core.AbstractVerticle#stop()
	 */
	@Override
	public void stop() throws Exception {
		// TODO Auto-generated method stub
		super.stop();
	}

	public static void main(String[] args) {

	}

}

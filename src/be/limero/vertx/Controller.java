package be.limero.vertx;

import java.util.Base64;

import be.limero.util.Cbor;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class Controller extends AbstractVerticle {
	private final static Logger log = LoggerFactory.getLogger(Controller.class);
	static Vertx vertx = Vertx.factory.vertx();
	private final static  EventBus eb = vertx.eventBus();

	@Override
	public void start(Future<Void> startFuture) {
		log.info("MyVerticle started!");
	}

	@Override
	public void stop(Future stopFuture) throws Exception {
		log.info("MyVerticle stopped!");
	}

	public static void main(String args[]) {
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");

		Verticle udp = new UdpVerticle();

		vertx.deployVerticle(new UdpVerticle());
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (int i = 0; i < 10; i++)
			eb.send("udp", new JsonObject().put("bool", true).put("fl", 1.23).put("bytes",
					Base64.getEncoder().encodeToString(new byte[] { 1, 2, 3, 4,5 ,6})));
	}

}

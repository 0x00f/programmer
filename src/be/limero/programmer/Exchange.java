package be.limero.programmer;

import java.util.HashMap;
import java.util.stream.Stream;

import be.limero.network.Reply;
import be.limero.network.Request;
import be.limero.network.Request.Cmd;
import be.limero.util.Cbor;

public class Exchange {
	static HashMap<Integer, Exchange> list = new HashMap<Integer, Exchange>();
	final static int expireTime = 10000;
	int id;
	long created;
	Request request;
	Reply reply;
	FunctionalInterface f;
	java.util.function.Consumer<Cbor> handler;

	void func(Cbor o) {

	}

	Exchange(Request req, java.util.function.Consumer<Cbor> hdlr) {
		request = req;
		created = System.currentTimeMillis();
		reply = null;
		id = req._id;
		handler = hdlr;
	}

	static void create(Request req, java.util.function.Consumer<Cbor> hdlr) {
		Exchange exchange = new Exchange(req, hdlr);
		list.put(req._id, exchange);
		exchange.handler.accept(null);
	}

	static void delete(int id) {
		list.remove(id);
	}
	
	static void findAndHandle(Cbor reply)  {
		
	}

	public static void main(String[] args) {
		XXX xxx = new XXX();
		Exchange.create(new Request(Cmd.EXEC, Stm32Protocol.Get()), xxx::on);
	}

}

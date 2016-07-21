package be.limero.programmer;

import java.util.HashMap;
import java.util.stream.Stream;

import be.limero.network.Reply;
import be.limero.network.Request;
import be.limero.network.Request.Cmd;
import be.limero.programmer.Stm32Controller.Route;
import be.limero.util.Cbor;

public class Exchange {
	static HashMap<Integer, Exchange> list = new HashMap<Integer, Exchange>();
	final static int expireTime = 10000;
	int id;
	long created;
	Request request;
	Reply reply;
	Route route;
	FunctionalInterface f;
	java.util.function.Consumer<Cbor> handler;

	void func(Cbor o) {

	}

	Exchange(Request req, java.util.function.Consumer<Cbor> hdlr) {
		request = req;
		created = System.currentTimeMillis();
		reply = null;
		id = req.id;
		handler = hdlr;
	}

	Exchange(Request req, Route rte) {
		request = req;
		created = System.currentTimeMillis();
		reply = null;
		id = req.id;
		route = rte;
	}

	static void create(Request req, java.util.function.Consumer<Cbor> hdlr) {
		Exchange exchange = new Exchange(req, hdlr);
		list.put(req.id, exchange);
//		exchange.handler.accept(null);
	}

	static void create(Request req, Route route) {
		Exchange exchange = new Exchange(req, route);
		list.put(req.id, exchange);
//		exchange.handler.accept(null);
	}

	static void delete(int id) {
		list.remove(id);
	}

	static Exchange find(int id) {
		return list.get(id);
	}

	public static void main(String[] args) {
		XXX xxx = new XXX();
		Exchange.create(new Request(Cmd.EXEC, Stm32Protocol.Get()), xxx::on);
	}

}

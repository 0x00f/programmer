package be.limero.vertx;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public class Exchange {
	JsonObject request;
	JsonObject response;
	int id;
	Handler<JsonObject> handler;
	Date date;
	static LinkedHashMap<Integer, Exchange> list = new LinkedHashMap<Integer, Exchange>();

	static void add(JsonObject request, Handler<JsonObject> handler) {
		Exchange exchange = new Exchange();
		exchange.id = request.getInteger("id");
		exchange.request = request;
		exchange.handler = handler;
		exchange.response = null;
		exchange.date = new Date();
		list.put(exchange.id, exchange);
	}

	Exchange findLowest() {
		Integer id = list.keySet().iterator().next();
		if (id == null)
			return null;
		return list.get(id);
	}

	static boolean handle(JsonObject response) {
		int id;
		if (response.containsKey("id")) {
			id = response.getInteger("id");
			if (list.containsKey(id)) {
				list.get(id).getHandler().handle(response);
			}
		}
		return false;
	}

	/**
	 * @return the request
	 */
	public JsonObject getRequest() {
		return request;
	}

	/**
	 * @param request
	 *            the request to set
	 */
	public void setRequest(JsonObject request) {
		this.request = request;
	}

	/**
	 * @return the response
	 */
	public JsonObject getResponse() {
		return response;
	}

	/**
	 * @param response
	 *            the response to set
	 */
	public void setResponse(JsonObject response) {
		this.response = response;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param id
	 *            the id to set
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @return the handler
	 */
	public Handler<JsonObject> getHandler() {
		return handler;
	}

	/**
	 * @param handler
	 *            the handler to set
	 */
	public void setHandler(Handler<JsonObject> handler) {
		this.handler = handler;
	}

	public static void main(String[] args) {
		JsonObject req = new JsonObject().put("id", 100).put("cmd", "get");

		add(req, resp -> myHandler(resp));
		handle(req);

	}

	private static Object myHandler(JsonObject resp) {
		System.out.println(resp);
		return null;
	}

}

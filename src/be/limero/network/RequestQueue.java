package be.limero.network;

import java.util.concurrent.ArrayBlockingQueue;

import io.vertx.core.json.JsonObject;

public class RequestQueue extends ArrayBlockingQueue<JsonObject> {

	public RequestQueue(int capacity) {
		super(capacity);		
	}

	public static void main(String[] args) {
		RequestQueue queue=new RequestQueue(100);
		 
		try {
			queue.put( new Request("hi",new byte[]{1,2,4,3}).toJson());
			JsonObject req =  queue.take();
			System.out.println(req);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}

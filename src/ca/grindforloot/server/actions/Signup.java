package ca.grindforloot.server.actions;

import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;

public class Signup extends Action{
	
	public Signup(NetSocket socket, JsonObject request) {
		super(socket, request);
		// TODO Auto-generated constructor stub
	}

	public void perform() {
		System.out.println("Success!");
		
		
		
	}

}

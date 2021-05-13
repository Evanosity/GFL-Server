package ca.grindforloot.server.actions;

import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;

public abstract class Action {
	
	protected final NetSocket socket;
	protected final JsonObject request;
	
	public Action(NetSocket socket, JsonObject request) {
		this.socket = socket;
		this.request = request;
		
		perform();
	}
	/**
	 * Given the request and response, do something.
	 */
	abstract void perform();
}

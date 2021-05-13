package ca.grindforloot.server.actions;

import ca.grindforloot.server.db.DBService;
import ca.grindforloot.server.errors.UserError;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;

public abstract class Action {
	
	protected final NetSocket socket;
	protected final JsonObject request;
	protected final DBService db;
	
	public Action(NetSocket socket, JsonObject request) {
		this.socket = socket;
		this.request = request;
		
		db = null;
		
	}
	/**
	 * Given the request and response, do something.
	 */
	public abstract void perform() throws UserError;
}

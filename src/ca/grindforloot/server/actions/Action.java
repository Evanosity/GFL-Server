package ca.grindforloot.server.actions;

import ca.grindforloot.server.Context;
import ca.grindforloot.server.db.DBService;
import ca.grindforloot.server.errors.UserError;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;

public abstract class Action {
	
	public Context context;
	public DBService db;
	
	public Action(Context context) {
		this.context = context;
		this.db = context.getDB();
		
	}
	/**
	 * Given the request and response, do something.
	 */
	public abstract void perform() throws UserError;
}

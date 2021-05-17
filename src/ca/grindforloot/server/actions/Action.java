package ca.grindforloot.server.actions;

import ca.grindforloot.server.GameContext;
import ca.grindforloot.server.db.DBService;
import ca.grindforloot.server.errors.UserError;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;

public abstract class Action {
	
	public GameContext ctx;
	public DBService db;
	
	public Action(GameContext ctx) {
		this.ctx = ctx;
		this.db = ctx.getDB();
		
	}
	/**
	 * Given the request and response, do something.
	 */
	public abstract void perform() throws UserError;
	
	/**
	 * Checks to see if the user is authenticated before allowing the action to proceed.
	 * 
	 * Override this method is this is an action where the user <i>doesn't</i> have to be authenticated(login, signup)
	 * 
	 * @throws UserError
	 */
	public void doChecks() throws UserError{
		if(ctx.session.isAuthenticated() == false)
			throw new UserError("Authentication Error", "You have to be logged in to do this.");
	}
}

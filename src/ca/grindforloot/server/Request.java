package ca.grindforloot.server;

import ca.grindforloot.server.db.DBService;

/**
 * Base class for a user's request.
 * @author Evan
 *
 */
public class Request {
	public final GameContext ctx;
	public final DBService db;
	
	public Request(GameContext ctx) {
		this.ctx = ctx;
		db = ctx.db;
	}
}

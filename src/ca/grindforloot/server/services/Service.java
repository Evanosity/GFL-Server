package ca.grindforloot.server.services;

import ca.grindforloot.server.GameContext;
import ca.grindforloot.server.db.DBService;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetSocket;

public class Service {
	
	public final GameContext ctx;
	public final DBService db;
	public final NetSocket socket;
	public final Vertx vertx;
	
	public Service(GameContext context) {
		this.ctx = context;
		db = context.getDB();
		socket = context.socket;
		vertx = context.vertx;
	}
}

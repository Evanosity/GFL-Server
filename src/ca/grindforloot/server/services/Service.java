package ca.grindforloot.server.services;

import ca.grindforloot.server.Context;
import ca.grindforloot.server.db.DBService;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetSocket;

public class Service {
	
	public final Context ctx;
	public final DBService db;
	public final NetSocket socket;
	public final Vertx vertx;
	
	public Service(Context context) {
		this.ctx = context;
		db = context.getDB();
		socket = context.socket;
		vertx = context.vertx;
	}
}

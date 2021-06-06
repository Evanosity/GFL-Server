package ca.grindforloot.server.controllers;

import ca.grindforloot.server.GameContext;
import ca.grindforloot.server.Request;
import io.vertx.core.json.JsonObject;

public abstract class Controller extends Request{
	public Controller(GameContext ctx) {
		super(ctx);
	}
	
	public abstract JsonObject process();
}

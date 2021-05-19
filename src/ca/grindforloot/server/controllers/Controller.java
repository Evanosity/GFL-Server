package ca.grindforloot.server.controllers;

import ca.grindforloot.server.GameContext;
import io.vertx.core.json.JsonObject;

public abstract class Controller {
	public final GameContext ctx;
	public Controller(GameContext ctx) {
		this.ctx = ctx;
	}
	
	public abstract JsonObject process();
}

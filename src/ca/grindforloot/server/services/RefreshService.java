package ca.grindforloot.server.services;

import ca.grindforloot.server.GameContext;
import ca.grindforloot.server.entities.Being;
import ca.grindforloot.server.entities.User;
import io.vertx.core.json.JsonObject;

public class RefreshService extends Service{

	public RefreshService(GameContext context) {
		super(context);
	}
	
	/**
	 * TODO do we want a custom object holding updates?
	 * @param character
	 * @param updates
	 */
	public void sendUpdatesToCharacter(Being character, JsonObject updates) {
		User user = character.getUser();
		
		//TODO reverse engineer the session ID from the user. Make sure that the character is actually open right now.
		//So, first make sure that the character is the active character of the user.
		//Then, grab the session associated with that user.
		//Do we store sessions in RAM?
		
		String sessionId = "";
		
		vertx.eventBus().publish("refresh." + sessionId, updates);
	}
}

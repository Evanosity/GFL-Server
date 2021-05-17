package ca.grindforloot.server.actions;

import ca.grindforloot.server.Context;
import ca.grindforloot.server.entities.Session;
import ca.grindforloot.server.entities.User;
import ca.grindforloot.server.errors.UserError;
import io.vertx.core.json.JsonObject;

public class Login extends Action{

	public Login(Context context) {
		super(context);
	}

	@Override
	public void perform() throws UserError {
		String hashedPassword = context.getStringProperty("password");
		Session session = context.session;
		
		User user = session.getUser();
		
		if(user.validatePassword(hashedPassword)) {
			session.authenticateFor(user);
			
			db.put(session);
			
			//TODO update the client
		}
		else {
			JsonObject result = new JsonObject();
			result.put("type", "error");
			result.put("message", "Invalid password");
			
			context.writeToSocket(result);
		}
	}
}

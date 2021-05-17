package ca.grindforloot.server.actions;

import ca.grindforloot.server.Context;
import ca.grindforloot.server.entities.Session;
import ca.grindforloot.server.entities.User;
import ca.grindforloot.server.errors.UserError;

public class Login extends Action{

	public Login(Context context) {
		super(context);
	}

	@Override
	public void perform() throws UserError {
		String hashedPassword = ctx.getStringProperty("password");
		Session session = ctx.session;
		
		User user = session.getUser();
		
		if(user.validatePassword(hashedPassword)) {
			session.authenticateFor(user);
			
			db.put(session);
			
			//TODO update the client
			//TODO preload old chat messages
		}
		else
			throw new UserError("Authentication Failed", "Invalid Password");
	}

	@Override
	public void doChecks() throws UserError {
		if(ctx.session.isAuthenticated())
			throw new UserError("Login Failed", "You are already logged in.");
	}
}

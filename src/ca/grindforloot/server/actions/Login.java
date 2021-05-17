package ca.grindforloot.server.actions;

import java.util.List;

import ca.grindforloot.server.GameContext;
import ca.grindforloot.server.db.QueryService;
import ca.grindforloot.server.db.QueryService.FilterOperator;
import ca.grindforloot.server.entities.Session;
import ca.grindforloot.server.entities.User;
import ca.grindforloot.server.errors.UserError;

public class Login extends Action{

	public Login(GameContext ctx) {
		super(ctx);
	}

	@Override
	public void perform() throws UserError {
		String hashedPassword = ctx.getStringProperty("password");
		String email = ctx.getStringProperty("email");
		
		QueryService qs = new QueryService(db);
		
		List<User> users = qs.fetchEntities("User", "email", FilterOperator.EQUAL, email);
		
		if(users.size() > 1) 
			throw new RuntimeException("Is this even possible?");
		if(users.size() == 0)
			throw new UserError("Auth Error", "Email not found");
		
		User user = users.get(0);	
		
		
		if(user.validatePassword(hashedPassword)) {
			
			Session session = ctx.session;
			
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

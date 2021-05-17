package ca.grindforloot.server.actions;

import ca.grindforloot.server.Context;
import ca.grindforloot.server.db.QueryService;
import ca.grindforloot.server.db.QueryService.FilterOperator;
import ca.grindforloot.server.entities.EntityService;
import ca.grindforloot.server.entities.Being;
import ca.grindforloot.server.entities.User;
import ca.grindforloot.server.errors.UserError;

public class Signup extends Action{
	
	public Signup(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public void perform() throws UserError {
		
		QueryService qs = new QueryService(db);
		EntityService es = new EntityService(db);
		
		String email = context.getStringProperty("username");
		String hashedPassword = context.getStringProperty("pass");
		String characterName = context.getStringProperty("characterName");
				
		if(qs.countEntities("User", "email", FilterOperator.EQUAL, email) > 0)
			throw new UserError("Email already created.");
		
		if(qs.countEntities("Character", "name", FilterOperator.EQUAL, characterName) > 0)
			throw new UserError("That character name is already in use.");
		
		//TODO enforce a regex on the character name and potentially the email as well?
		
		User user = es.createEntity("User");
		user.setEmail(email);
		user.setPassword(hashedPassword);
		
		Being character = es.createEntity("Character");
		
		db.put(user, character);
		
	}

}

package ca.grindforloot.server.actions;

import ca.grindforloot.server.db.Entity;
import ca.grindforloot.server.errors.UserError;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;

public class Signup extends Action{
	
	public Signup(NetSocket socket, JsonObject request) {
		super(socket, request);
		// TODO Auto-generated constructor stub
	}

	public void perform() throws UserError {
		System.out.println("Success!");
		
		String email = request.getString("username");
		String hashedPassword = request.getString("pass");
		String characterName = request.getString("characterName");
		
		boolean x = false;
		
		if(x)
			throw new UserError("Email already created.");
		
		if(x)
			throw new UserError("That character name is already in use.");
		
		Entity user = db.createEntity("User");
		
		db.put(user);
		
	}

}

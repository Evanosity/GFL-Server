package ca.grindforloot.server.entities;

import java.util.Set;

import org.bson.Document;

import ca.grindforloot.server.db.DBService;
import ca.grindforloot.server.db.Entity;
import ca.grindforloot.server.db.Key;

public class Session extends Entity{
	
	protected Session(DBService db, Document raw, boolean isNew, Set<String> projections) {
		super(db, raw, isNew, projections);
		// TODO Auto-generated constructor stub
	}
	
	public boolean isAuthenticated() {
		return raw.getBoolean("authenticated", false);
	}
	
	public void authenticateFor(User user) {
		setValue("authenticated", true);
		setValue("userKey", user.getKey());
	}
	
	public User getUser() {
		if(isAuthenticated()) {
			Key key = getKeyValue("userKey");
			
			//wow, i love this api
			return db.getEntity(key);
		}
		else 
			return null;
	}

	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return "Session";
	}
}

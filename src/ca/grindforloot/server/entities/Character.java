package ca.grindforloot.server.entities;

import java.util.Set;

import org.bson.Document;

import ca.grindforloot.server.db.DBService;
import ca.grindforloot.server.db.Entity;
import ca.grindforloot.server.db.Key;

public class Character extends Entity{

	protected Character(DBService db, Document raw, boolean isNew, Set<String> projections) {
		super(db, raw, isNew, projections);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return "Character";
	}
	
	public User getUser() {
		Key userKey = getKeyValue("userKey");
		
		return (User) db.getEntity(userKey);
	}

}

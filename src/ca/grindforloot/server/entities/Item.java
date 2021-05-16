package ca.grindforloot.server.entities;

import java.util.Set;

import org.bson.Document;

import ca.grindforloot.server.db.DBService;
import ca.grindforloot.server.db.Entity;

public class Item extends Entity{

	protected Item(DBService db, Document raw, boolean isNew, Set<String> projections) {
		super(db, raw, isNew, projections);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return "Item";
	}
	
	public Entity getParent() {
		return null;
	}

}

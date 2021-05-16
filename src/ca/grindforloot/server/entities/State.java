package ca.grindforloot.server.entities;

import java.util.Set;

import org.bson.Document;

import ca.grindforloot.server.db.DBService;
import ca.grindforloot.server.db.Entity;

public class State extends Entity{
	
	protected State(DBService db, Document raw, boolean isNew, Set<String> projections) {
		super(db, raw, isNew, projections);
		// TODO Auto-generated constructor stub
	}

	public boolean isPermanent() {
		return raw.getBoolean("permanent", true);
	}

	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return "State";
	}
}

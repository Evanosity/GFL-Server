package ca.grindforloot.server.entities;

import java.util.Set;

import org.bson.Document;

import ca.grindforloot.server.db.DBService;
import ca.grindforloot.server.db.Entity;

public class Script extends Entity{

	protected Script(DBService db, Document raw, boolean isNew, Set<String> projections) {
		super(db, raw, isNew, projections);
		// TODO Auto-generated constructor stub
	}
	
	public String getScript() {
		return (String) getValue("script");
	}
	
	public boolean isCron() {
		return getValue("cron") != null && (Long) getValue("cron") != 0;
	}
	
	public Long getCronTimer() {
		if(false == isCron())
			return null;
		
		return (Long) getValue("cron");
	}

	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return "Script";
	}

}

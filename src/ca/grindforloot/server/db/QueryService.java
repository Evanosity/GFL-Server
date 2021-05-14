package ca.grindforloot.server.db;

import java.util.List;

import org.bson.conversions.Bson;


public class QueryService {
	protected final DBService db;
	
	public QueryService(DBService db) {
		this.db = db;
	}
	
	public List<Entity> runEntityQuery(Query q){	
		Bson filter = db.generateCompositeFilter(q.filters);
		
		return db.fetchInternal(q.getType(), filter);
	}
	public void runDeleteQuery(Query q) {
		
	}
	public void runModifyQuery(Query q) {
		
	}
}

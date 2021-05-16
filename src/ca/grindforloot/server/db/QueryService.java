package ca.grindforloot.server.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;

import ca.grindforloot.server.Pair;


/**
 * Wraps DB service to allow Query objects to be passed in. 
 * This service should be used when you <i>don't</i> have the key(s) of entities you want to retrieve.
 * If you do, use {@link DBService.getEntity} and {@link DBService.getEntities}
 * TODO those links are toast rip
 * 
 * @author Evan
 *
 */
public class QueryService {
	protected final DBService db;
	
	public enum FilterOperator {
		EQUAL, NOT_EQUAL, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL
	}
	
	public QueryService(DBService db) {
		this.db = db;
	}
	
	public List<Entity> fetchEntites(String type, String field, FilterOperator op, Object value){
		Query q = new Query(type);
		q.addFilter(field, op, value);
		
		return runEntityQuery(q);
	}
	
	public Long countEntities(String type, String field, FilterOperator op, Object value) {
		Query q = new Query(type);
		q.addFilter(field, op, value);
		
		return runCount(q);
	}
	
	public List<Entity> runEntityQuery(Query q){	
		Bson filter = generateCompositeFilter(q.filters); 
		
		return db.fetchInternal(q.getType(), filter, q.projections);
	}
	public void runDeleteQuery(Query q) {
		Bson filter = generateCompositeFilter(q.filters);
		
		db.db.getCollection(q.getType()).deleteMany(filter);
		
	}
	public void runUpdate(Query q) {
		Bson filters = generateCompositeFilter(q.filters);
		Bson updates = generateUpdates(q.updates);
		
		db.db.getCollection(q.getType()).updateMany(filters, updates);
	}
	public Long runCount(Query q) {
		Bson filters = generateCompositeFilter(q.filters);
		
		return db.db.getCollection(q.getType()).countDocuments(filters);
	}
	
	protected static Bson generateProjections(Set<String> projections) {
		
		return Projections.include(projections.toArray(new String[projections.size()]));	
	}
	
	protected static Bson generateUpdates(Map<String, Object> updates) {
		List<Bson> bsonUpdates = new ArrayList<>();
		
		for(Entry<String, Object> entry : updates.entrySet()) 
			bsonUpdates.add(Updates.set(entry.getKey(), entry.getValue()));
		
		
		return Updates.combine(bsonUpdates);
	}
	
	
	
	//********** FILTERS **********
	
	/**
	 * Generates a Bson filter for a singular ID.
	 * @param id
	 * @return
	 */
	protected static Bson getFilterForId(String id) {
		return Filters.eq("_id", new ObjectId(id));
	}
	
	/**
	 * Generate a composite bson filter
	 * TODO helper methods for queries
	 * @param a string-<filteroperator-object> map of the filters. see Query and QueryService for impl
	 * @return the composed Bson
	 */
	protected static Bson generateCompositeFilter(Map<String, Pair<FilterOperator, Object>> filters) {
		
		List<Bson> builtFilters = new ArrayList<>();
		
		for(Entry<String, Pair<FilterOperator, Object>> entry : filters.entrySet()) {
			String type = entry.getKey();
			Pair<FilterOperator, Object> rawFilter = entry.getValue();
			
			builtFilters.add(generateFilter(type, rawFilter.getKey(), rawFilter.getValue()));
		}
				
		//Compose the final filter.
		//TODO clean this up maybe?
		return Filters.and(builtFilters.toArray(new Bson[builtFilters.size()]));
	}
	
	/**
	 * Generate a SINGLE bson filter
	 * @param fieldName - the field we're operating for
	 * @param op - the FilterOperator we're using(Equal, Not equal, etc)
	 * @param value - the value we're comparing against
	 * @return the bson filter
	 */
	protected static Bson generateFilter(String fieldName, FilterOperator op, Object value) {
		switch(op) {
		case EQUAL:
			return Filters.eq(fieldName, value);
		case NOT_EQUAL:
			return Filters.ne(fieldName, value);
		case GREATER:
			return Filters.gt(fieldName, value);
		case GREATER_EQUAL:
			return Filters.gte(fieldName, value);
		case LESS:
			return Filters.lt(fieldName, value);
		case LESS_EQUAL:
			return Filters.lte(fieldName, value);
		default:
			throw new IllegalArgumentException("Invalid filter operator " + op.toString());
		}
	}
}
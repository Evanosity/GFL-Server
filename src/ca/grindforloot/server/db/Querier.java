package ca.grindforloot.server.db;

import java.util.List;

/**
 * Wraps QueryService to provide helper methods for fetching queries.
 * 
 * For lower level functionality, adding projections, updating, deleting, etc, you can just use QueryService
 * 
 * @author Evan
 *
 */
public class Querier extends QueryService{
	
	private Query query = null;

	public Querier(DBService db) {
		super(db);
	}
	
	public <T extends Entity> List<T> fetchEntities(String type, String field, FilterOperator op, Object value){
		
		Query q = getQuery(type).addFilter(field, op, value);
		
		List<T> result = runEntityQuery(q);
		query = null;
		
		return result;
	}
	public <T extends Entity> List<T> fetchEntities(String type, String field, FilterOperator op, Object value,
			String field2, FilterOperator op2, Object value2){
		
		getQuery(type).addFilter(field2, op2, value2);
		
		return fetchEntities(type, field, op, value);
	}
	public <T extends Entity> List<T> fetchEntities(String type, String field, FilterOperator op, Object value,
			String field2, FilterOperator op2, Object value2,
			String field3, FilterOperator op3, Object value3){
		
		getQuery(type).addFilter(field3, op3, value3);
		
		return fetchEntities(type, field, op, value, field2, op2, value2);
	}
	

	public Long countEntities(String type, String field, FilterOperator op, Object value){
		
		Query q = getQuery(type).addFilter(field, op, value);
		
		Long result = runCount(q);
		query = null;
		
		return result;
	}
	public Long countEntities(String type, String field, FilterOperator op, Object value,
			String field2, FilterOperator op2, Object value2){
		
		getQuery(type).addFilter(field2, op2, value2);
		
		return countEntities(type, field, op, value);
	}
	public Long countEntities(String type, String field, FilterOperator op, Object value,
			String field2, FilterOperator op2, Object value2,
			String field3, FilterOperator op3, Object value3){
		
		getQuery(type).addFilter(field3, op3, value3);
		
		return countEntities(type, field, op, value, field2, op2, value2);
	}
	
	//TODO consider methods for updating and deleting entities
	
	private Query getQuery(String type) {
		if(query == null)
			return new Query(type);
		else
			return query;
	}
}

package ca.grindforloot.server.db;

import java.util.HashMap;
import java.util.Map;

import ca.grindforloot.server.Duple;
import ca.grindforloot.server.db.QueryService.FilterOperator;

/**
 * This stores information about a query before it is executed. A logical building block in efficient queries
 * @author Evan
 *
 */
public class Query {
	protected Map<String, Duple<FilterOperator, Object>> filters = new HashMap<>();
	protected Map<String, Object> updates = new HashMap<>();
	
	private final String type;
	
	
	public Query(DBService db, String type) {
		this.type = type;
	}
	
	public String getType() {
		return type;
	}
	
	/**
	 * Some queries will modify entities in the DB instead of pulling them
	 * @param propertyName
	 * @param newValue
	 * @return itself
	 */
	public Query addUpdate(String propertyName, Object newValue) {
		
		updates.put(propertyName, newValue);
		
		return this;
	}
	
	/**
	 * Remove a modify query value
	 * @param propertyName
	 * @return itself
	 */
	public Query removeUpdate(String propertyName) {
		updates.remove(propertyName);
		return this;
	}
	
	/**
	 * 
	 * @param propertyName
	 * @param operator
	 * @param value
	 * @return itself
	 */
	public Query addFilter(String propertyName, FilterOperator operator, Object value) {
		Duple<FilterOperator, Object> duple = new Duple<>(operator, value);
		
		filters.put(propertyName, duple);
		
		return this;
	}
	
	public Query removeFilter(String propertyName) {
		filters.remove(propertyName);
		
		return this;
	}
}

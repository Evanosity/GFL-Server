package ca.grindforloot.server.db;

import java.util.List;

import org.bson.Document;

public abstract class Entity {
	public DBService db;
	protected Document raw;
	private final Key key;
	
	private final boolean isNew;
		
	protected Entity(DBService db, Document raw) {
		this(db, raw, false);
	}
	
	protected Entity(DBService db, Document raw, boolean isNew) {
		this.db = db;
		this.raw = raw;
		this.isNew = isNew;
		
		//TODO set the ID of a new document
				
		this.key = new Key(getType(), raw.getObjectId("_id").toHexString());
		
		assert getType().equals(key.getType());
	}
	
	public abstract String getType();
		
	protected Object getValue(String key) {
		return raw.get(key);
	}
	
	protected void setValue(String key, Object value) {
		raw.put(key, parseValue(value));
	}
	
	/**
	 * Embeds a key onto an entity
	 * @param property
	 * @param key
	 */
	protected void setKeyValue(String property, Key key) {
		setValue(property, key);
	}
	
	protected Key getKeyValue(String property) {
		Document rawKey = (Document) getValue(property);
		
		return new Key(rawKey);
		
	}
		
	public boolean hasValue(String key) {
		return raw.containsKey(key);
	}
	
	public Key getKey() {
		return key;
	}
	
	public String getId() {
		return getKey().getId();
	}
	
	public boolean isNew() {
		return isNew;
	}
	
	/**
	 * Convert raw objects into the appropriate storage format for mongodb.
	 * Notably, key -> document
	 * This is its own method because in the cast of lists, it calls itself recursively.
	 * @param obj
	 * @return
	 */
	private static Object parseValue(Object obj) {
		if(obj instanceof Key) {
			Key key = (Key) obj;
			return key.toDocument();
		}
		if(obj instanceof List) {
			List<?> list = (List<?>) obj;
			
			for(Object o : list) 
				return parseValue(o);
			
		}
		
		return obj;
	}
}

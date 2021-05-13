package ca.grindforloot.server.db;

import org.bson.Document;

public abstract class Entity {
	public DBService db;
	protected Document raw;
	private Key key;
	
	private boolean isNew = true;
		
	protected Entity(DBService db, Document raw) {
		this.db = db;
		this.raw = raw;
		isNew = false;
	}
	
	protected Entity(DBService db, Document raw, boolean isNew) {
		this.db = db;
		this.raw = raw;
		
		this.isNew = isNew;
	}
		
	protected Object getValue(String key) {
		return raw.get(key);
	}
	
	protected void setValue(String key, Object value) {
		raw.put(key, value);
	}
	
	protected void setKeyValue(String property, Key key) {
		raw.put(property, db.keyToDocument(key));
	}
	
	//TODO
	protected void setArrayValue(String property, List<?> value) {
		
	}
	
	public boolean hasValue(String key) {
		return raw.containsKey(key);
	}
	
	public Key getKey() {
		return key;
	}
	
	public String getType() {
		return getKey().getType();
	}
	public String getId() {
		return getKey().getId();
	}
	
	public boolean isNew() {
		return isNew;
	}
}

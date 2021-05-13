package ca.grindforloot.server.db;

import java.util.ArrayList;
import java.util.List;

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
	
	/**
	 * Embeds a key onto an entity
	 * @param property
	 * @param key
	 */
	protected void setKeyValue(String property, Key key) {
		raw.put(property, db.keyToDocument(key));
	}
	
	protected Key getKeyValue(String property) {
		Document rawKey = (Document) raw.get(property);
		
		return db.documentToKey(rawKey);
		
	}
	
	protected <T> void setListValue(String property, List<T> list) {
		//TODO what if T is a key?
		raw.put(property, list);
	}
	
	protected <T> List<T> getListValue(String property, Class<T> clazz){
		return raw.getList(property, clazz, new ArrayList<T>());
		//TODO what if T is a key?
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

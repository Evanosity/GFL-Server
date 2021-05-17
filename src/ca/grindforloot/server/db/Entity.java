package ca.grindforloot.server.db;

import java.util.Map.Entry;
import java.util.Set;

import org.bson.Document;
import org.bson.types.ObjectId;

import io.vertx.core.json.JsonObject;

/**
 * Abstract class, the root of the entity system. Wraps a MongoDB Document with helper methods
 * 
 * All subclasses should implement every constructor.
 * 
 * You do NOT need to provide a key object to this entity; it gets generated upon creation.
 * 
 * @author Evan
 *
 */
public abstract class Entity {
	protected DBService db;
	protected Document raw;
	private final Key key;
	private final Set<String> projections;
	
	private final boolean isNew;
	
	//Normal constructor
	protected Entity(DBService db, Document raw) {
		this(db, raw, false, null);
	}
	
	//new entity
	protected Entity(DBService db, Document raw, boolean isNew) {
		this(db, raw, true, null);
	}
	
	//entity with projections. Here, it's implied that the entity is NOT new.
	protected Entity(DBService db, Document raw, Set<String> projections) {
		this(db, raw, false, projections);
	}
	
	//internal constructor
	protected Entity(DBService db, Document raw, boolean isNew, Set<String> projections) {
		this.db = db;
		this.raw = raw;
		this.isNew = isNew;
		
		this.projections = projections;

		//if the entity is phresh, we need to generate a key for it
		if(isNew) {
			this.key = db.generateKey(getType());
			raw.put("_id", new ObjectId(key.getId()));
		}
		//otherwise, create the key out of the type and ID.
		else {
			this.key = new Key(getType(), raw.getObjectId("_id").toHexString());
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public abstract String getType();
	
	public boolean isNew() {
		return isNew;
	}
	
	public boolean projected() {
		return projections != null && false == projections.isEmpty();
	}
	
	/**
	 * This will return null if no projections were set. or at least it should....
	 * @return
	 */
	public Set<String> getProjections(){
		return projections;
	}
	
	public String getName() {
		return raw.getString("name");
	}
	
	protected Object getValue(String key) {
		return raw.get(key);
	}
	
	protected void setValue(String key, Object value) {
		raw.put(key, DBService.parseValue(value));
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
	
	/**
	 * TODO once the schema is setup, split this into two methods.
	 * One that reads if the entity's schema has a certain field
	 * One that reads if this entity has a non-null value for a field
	 * @param key
	 * @return
	 */
	public boolean hasValue(String key) {
		return raw.containsKey(key);
	}
	
	public Key getKey() {
		return key;
	}
	
	public String getId() {
		return getKey().getId();
	}
	

	
	/**
	 * Create a Vert.x JsonObject representation of this entity
	 * @return
	 */
	public JsonObject toJson() {
		JsonObject result = new JsonObject();
		
		for(Entry<String, Object> entry : raw.entrySet())
			result.put(entry.getKey(), entry.getValue());
		
		return result;
	}
}

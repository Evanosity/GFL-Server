package ca.grindforloot.server.db;

import org.bson.Document;
import org.bson.types.ObjectId;

/**
 * This contains an immutable reference to an Entity, by combining its type(collection) and id.
 * @author Evan
 * 
 * TODO consider if we want a method to encode the entire key
 *
 */
public class Key {
	private final String type;
	private final String id;
	protected Key(String type, String id) {
		this.type = type;
		this.id = id;
	}
	protected Key(Document doc) {
		type = doc.getString("type");
		id = doc.getString("id");
	}
	protected Key(String type, ObjectId id) {
		this.type = type;
		this.id = id.toHexString();
	}
	
	/**
	 * Turns this into a document; this is to store it on another document.
	 * @param key
	 * @return
	 */
	protected Document toDocument() {
		Document result = new Document();
		
		result.put("type", type);
		result.put("id", id);
		
		return result;
	}
	
	public String getType() {
		return type;
	}
	public String getId() {
		return id;
	}
}

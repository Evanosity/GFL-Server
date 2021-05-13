package ca.grindforloot.server.db;

/**
 * This contains an immutable reference to an Entity, by combining its type(collection) and id.
 * @author Evan
 *
 */
public class Key {
	private String type;
	private String id;
	public Key(String type, String id) {
		this.type = type;
		this.id = id;
	}
	
	public String getType() {
		return type;
	}
	public String getId() {
		return id;
	}
}

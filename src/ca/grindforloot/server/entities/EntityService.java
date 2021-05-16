package ca.grindforloot.server.entities;

import java.util.Set;

import org.bson.Document;

import ca.grindforloot.server.db.DBService;
import ca.grindforloot.server.db.Entity;
import ca.grindforloot.server.db.Key;

/**
 * This might be necessary?? So we can avoid public constructors on entities
 * @author Evan
 *
 */
public class EntityService {
	
	public DBService db;
	
	public EntityService(DBService db) {
		this.db = db;
	}
	
	/**
	 * Create a new entity
	 * @param <T> the type of object we're creating
	 * @param type the string of the object we're creating If T and Type don't match, that's cringe.
	 * @return
	 */
	public <T extends Entity> T createEntity(String type) {
		Key key = db.generateKey(type);
		
		return createEntityObject(key, new Document(), true, null);
	}
	
	public <T extends Entity> T createEntityObject(Key key, Document doc) {
		return createEntityObject(key, doc, false, null);
	}
	
	public <T extends Entity> T createEntityObject(Key key, Document doc, Set<String> projections) {
		return createEntityObject(key, doc, false, projections);
	}
	
	@SuppressWarnings("unchecked")
	private <T extends Entity> T createEntityObject(Key key, Document doc, Boolean isNew, Set<String> projections) {
		switch(key.getType()) {
			case "Character":
				return (T) new Character(db, doc, isNew, projections);
			case "User":
				return (T) new User(db, doc, isNew, projections);
		default:
			throw new IllegalArgumentException("Entity type" + key.getType() + " is not supported.");
		}
	}
}

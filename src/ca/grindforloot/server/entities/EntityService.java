package ca.grindforloot.server.entities;

import java.util.Set;

import org.bson.Document;

import ca.grindforloot.server.db.DBService;
import ca.grindforloot.server.db.Entity;
import ca.grindforloot.server.db.Key;

/**
 * This is an extension on DBService that is used for generating entity objects.
 * It is in its own class so that it can be in the package with the rest of the entity types. All of their constructors are protected,
 * and this class is the only place you can create entity objects.
 * 
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
	
	public <T extends Entity> T buildEntity(Key key, Document doc) {
		return createEntityObject(key, doc, false, null);
	}
	
	public <T extends Entity> T buildEntity(Key key, Document doc, Set<String> projections) {
		return createEntityObject(key, doc, false, projections);
	}
	
	@SuppressWarnings("unchecked")
	private <T extends Entity> T createEntityObject(Key key, Document doc, Boolean isNew, Set<String> projections) {
		T result = null;
		switch(key.getType()) {
			case "Character":
				result = (T) new Being(db, doc, isNew, projections);
				break;
			case "User":
				result = (T) new User(db, doc, isNew, projections);
				break;
			case "Item":
				result = (T) new Item(db, doc, isNew, projections);
				break;
			case "State":
				result = (T) new State(db, doc, isNew, projections);
				break;
			case "Connection":
				result = (T) new Connection(db, doc, isNew, projections);
				break;
			default:
				throw new IllegalArgumentException("Entity type" + key.getType() + " is not supported.");
		}
		
		assert key.getType() == result.getType();
		
		return result;
	}
}

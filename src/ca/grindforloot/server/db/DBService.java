package ca.grindforloot.server.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

import ca.grindforloot.server.Utils;
import ca.grindforloot.server.Duple;

/**
 * The main point of access for DB Access.
 * My goal is to not expose the MongoDB api outside of this package.
 * @author Evan
 *
 */
public class DBService {
	final MongoClient client;
	final MongoDatabase db;
	
	public enum FilterOperator {
		EQUAL, NOT_EQUAL, GREATER, GREATER_EQUAL, LESS, LESS_EQUAL
	}
	
	
	public DBService(MongoClient client) {
		this.client = client;
		//db = client.getDatabase("GFL");
		db = null;
		
	}
	
	public Key getKey(String type, String id) {
		return new Key(type, id);
	}
	
	public Key generateKey(String type) {
		
		boolean resolved = false;
		
		ObjectId random = null;
		
		while(!resolved) {
			 random = new ObjectId();
			
			//TODO ensure it was unique. Do we put it to the DB if it was, just to keep its spot?
			
			resolved = true;
		}
		
		
		return new Key(type, random.toHexString());
	}
	
	/**
	 * Turns a key into a document; this is to store the key on another document.
	 * @param key
	 * @return
	 */
	protected Document keyToDocument(Key key) {
		Document result = new Document();
		result.put("type", key.getType());
		result.put("id", key.getId());
		
		return result;
	}
	
	protected Key documentToKey(Document doc) {
		String type = doc.getString("type");
		String id = doc.getString("id");
		
		return getKey(type, id);
	}
	
	/**
	 * Create a new, blank entity.
	 * TODO generate a key with a unique identifier
	 * @param type
	 * @return
	 */
	public Entity createEntity(String type) {
		Key key = generateKey(type);
		
		return createEntityObject(key, new Document(), true);
	}
	
	protected Entity createEntityObject(Key key, Document doc) {
		return createEntityObject(key, doc, false);
	}
	
	/**
	 * Instantiates an entity object, given its type. Will implode if the type doesnt exist
	 * @param key
	 * @param doc
	 * @param isNew
	 * @return
	 */
	protected Entity createEntityObject(Key key, Document doc, Boolean isNew) {
		Object[] values = {this, doc, isNew};
				
		try {
			return (Entity) Utils.instantiate("com.grindforloot.entities." + key.getType(), values);
		}
		catch(Exception e) {
			throw new RuntimeException("Entity type " + key.getType() + " does not exist!" + e.getStackTrace());
		}
	}
	
	public void put(Entity ent) {
		MongoCollection<Document> col = db.getCollection(ent.getType());
		
		if(ent.isNew())
			col.insertOne(ent.raw);
		else {
			col.replaceOne(getFilterForId(ent.getId()), ent.raw);
		}
	}
	/**
	 * Insert a list of entities into the database
	 * @param ents
	 */
	public void put(Iterable<Entity> ents) {
		Map<String, List<Entity>> sorted = sortEntitiesByType(ents);
		
		for(Entry<String, List<Entity>> entry : sorted.entrySet()) {
			MongoCollection<Document> col = db.getCollection(entry.getKey());
						
			col.insertMany(getRawEntities(entry.getValue()));
		}
	}
	
	public void delete(Key key) {
		db.getCollection(key.getType()).deleteOne(getFilterForId(key.getId()));
	}
	/**
	 * this is inefficient AF. if youre gonna be deleting entities en masse, delete them with a Bson Filter.
	 * @param keys
	 */
	public void delete(Iterable<Key> keys) {
		for(Key key : keys) {
			db.getCollection(key.getType()).deleteOne(getFilterForId(key.getId()));
		}
	}
	
	private void putInternal() {
		
	}
	private void deleteInternal() {
		
	}
	
	public Entity getEntity(Key key) {
		
		List<Document> docs = fetchRawInternal(key.getType(), getFilterForId(key.getId()));
		
		if(docs.size() != 1)
			throw new IllegalStateException("cant have multiple docs with the same identifier. delete this project.");
		
		return createEntityObject(key, docs.get(0));
		
	}
	
	/**
	 * Returns a list of built entities, given a type and a Bson Filter
	 * The important part of this method is that we generate key objects for each parameter.
	 * @param type
	 * @param filter
	 * @return
	 */
	protected List<Entity> fetchInternal(String type, Bson filter){
		List<Document> rawList = fetchRawInternal(type, filter);
		List<Entity> result = new ArrayList<>();
		
		for(Document doc : rawList) {
			Key key = new Key(type, doc.getObjectId("_id").toHexString());
			
			result.add(createEntityObject(key, doc));
		}
		
		return result;
	}
	
	/**
	 * Fetches a list of documents from the DB, given a type and query.
	 * @param type
	 * @param filter
	 * @return
	 */
	private List<Document> fetchRawInternal(String type, Bson filter){
		MongoCollection<Document> col = db.getCollection(type);
		List<Document> result = new ArrayList<>();
		
		FindIterable<Document> dbResult = col.find(filter);
		
		for(Document doc : dbResult) {
			result.add(doc);
		}
		
		return result;
	}
	
	/**
	 * Generate a composite bson filter
	 * TODO helper methods for queries
	 * @param filters a string-object-filteroperator map of the filters.
	 * @return the composed Bson
	 */
	protected Bson generateCompositeFilter(Map<String, Duple<FilterOperator, Object>> filters) {
		
		List<Bson> builtFilters = new ArrayList<>();
		
		for(Entry<String, Duple<FilterOperator, Object>> entry : filters.entrySet()) {
			String type = entry.getKey();
			Duple<FilterOperator, Object> rawFilter = entry.getValue();
			
			builtFilters.add(generateFilter(type, rawFilter.getKey(), rawFilter.getValue()));
		}
				
		//Compose the final filter.
		//TODO clean this up maybe?
		return Filters.and(builtFilters.toArray(new Bson[builtFilters.size()]));
	}
	
	/**
	 * Generate a SINGLE bson filter
	 * @param fieldName - the field we're operating for
	 * @param op - the FilterOperator we're using(Equal, Not equal, etc)
	 * @param value - the value we're comparing against
	 * @return
	 */
	protected Bson generateFilter(String fieldName, FilterOperator op, Object value) {
		switch(op) {
		case EQUAL:
			return Filters.eq(fieldName, value);
		case NOT_EQUAL:
			return Filters.ne(fieldName, value);
		case GREATER:
			return Filters.gt(fieldName, value);
		case GREATER_EQUAL:
			return Filters.gte(fieldName, value);
		case LESS:
			return Filters.lt(fieldName, value);
		case LESS_EQUAL:
			return Filters.lte(fieldName, value);
		default:
			throw new IllegalArgumentException("Invalid filter operator");
		}
	}
	
	/**
	 * Generates a Bson filter for a singular ID.
	 * @param id
	 * @return
	 */
	private Bson getFilterForId(String id) {
		return Filters.eq("_id", new ObjectId(id));
	}
	
	/**
	 * Extracts a list of documents from a list of entities
	 * @param entities
	 * @return
	 */
	private List<Document> getRawEntities(List<Entity> entities){
		List<Document> result = new ArrayList<>();
		
		for(Entity ent : entities) 
			result.add(ent.raw);
		
		return result;
		
	}
	
	/**
	 * Sorts a list of entities into a map of entity type-entity
	 * @param ents
	 * @return
	 */
	private Map<String, List<Entity>> sortEntitiesByType(Iterable<Entity> ents){
		Map<String, List<Entity>> result = new HashMap<>();
		
		for(Entity ent : ents) {
			String type = ent.getType();
			
			List<Entity> current = result.get(type);
			if(current == null) current = new ArrayList<>();
			
			current.add(ent);
			
			result.put(type, current);
		}
		
		return result;
	}
}
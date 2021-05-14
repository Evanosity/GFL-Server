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

import ca.grindforloot.server.Utils;

/**
 * The main point of access for DB Access.
 * My goal is to not expose the MongoDB api outside of this package.
 * @author Evan
 *
 * TODO consider caching the collections we've called?
 *
 */
public class DBService {
	//private final MongoClient client;
	protected final MongoDatabase db;
	
	public DBService(MongoClient client) {
		//this.client = client;
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
			
			if(fetchInternal(type, QueryService.getFilterForId(random.toHexString())).size() == 0)
				resolved = true;
		}
		
		
		return new Key(type, random.toHexString());
	}
	
	/**
	 * Create a new, blank entity of the given type.
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
	 * Reflectively instantiates an entity object, given its type. Will implode if the type doesnt exist
	 * I feel like I'm gonna regret doing this reflectively. Infact i feel like i'm gonna regret doing anything reflectively.
	 * TODO consider if we want to be passing the type in here.
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
		
		putInternal(ent, col);
	}
	/**
	 * Insert a list of entities into the database
	 * @param ents
	 */
	public void put(Iterable<Entity> ents) {
		Map<String, List<Entity>> sorted = sortEntitiesByType(ents);
		
		for(Entry<String, List<Entity>> entry : sorted.entrySet()) {
			MongoCollection<Document> col = db.getCollection(entry.getKey());
						
			for(Entity ent : entry.getValue())
				putInternal(ent, col);
		}
	}
	
	/**
	 * Put an entity to the DB.
	 * @param ent - the Entity to put
	 * @param col - the MongoCollection we're putting this document to
	 */
	private void putInternal(Entity ent, MongoCollection<Document> col) {
		if(ent.isNew())
			col.insertOne(ent.raw);
		else
			col.replaceOne(QueryService.getFilterForId(ent.getId()), ent.raw);
	}
	
	public void delete(Key key) {
		db.getCollection(key.getType()).deleteOne(QueryService.getFilterForId(key.getId()));
	}
	/**
	 * Delete a set of entiites by their key.
	 * @param keys
	 */
	public void delete(Iterable<Key> keys) {
		
		Map<String, List<Key>> sorted = sortKeysByType(keys);
		
		for(Entry<String, List<Key>> entry : sorted.entrySet()) {
			MongoCollection<Document> col = db.getCollection(entry.getKey());
			
			for(Key key : entry.getValue())
				deleteInternal(key, col);
		}
	}
	
	private void deleteInternal(Key key, MongoCollection<Document> col) {
		col.deleteOne(QueryService.getFilterForId(key.getId()));
	}
	
	public Entity getEntity(Key key) {
		
		List<Document> docs = fetchRawInternal(key.getType(), QueryService.getFilterForId(key.getId()));
		
		if(docs.size() != 1)
			throw new IllegalStateException("cant have multiple docs with the same identifier. delete this project.");
		
		return createEntityObject(key, docs.get(0));
		
	}
	
	/**
	 * Returns a list of built entities, given a type and a Bson Filter. Any group fetch runs through this method.
	 * We create the entities using this.
	 * @param type
	 * @param filter
	 * @return
	 */
	protected List<Entity> fetchInternal(String type, Bson filter){
		List<Document> rawList = fetchRawInternal(type, filter);
		List<Entity> result = new ArrayList<>();
		
		for(Document doc : rawList) {
			Key key = new Key(type, doc.getObjectId("_id"));
			
			result.add(createEntityObject(key, doc));
		}
		
		return result;
	}
	
	/**
	 * Fetches a raw list of documents from the given collection
	 * @param collection
	 * @param filter
	 * @return
	 */
	private List<Document> fetchRawInternal(String collection, Bson filter){
		List<Document> result = new ArrayList<>();
		
		FindIterable<Document> dbResult = db.getCollection(collection).find(filter);
		
		for(Document doc : dbResult) 
			result.add(doc);
		
		return result;
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
	private Map<String, List<Key>> sortKeysByType(Iterable<Key> keys){
		Map<String, List<Key>> result = new HashMap<>();
		
		for(Key key : keys) {
			String type = key.getType();
			
			List<Key> current = result.get(type);
			if(current == null) current = new ArrayList<>();
			
			current.add(key);
			
			result.put(type, current);
		}
		
		return result;
	}
}

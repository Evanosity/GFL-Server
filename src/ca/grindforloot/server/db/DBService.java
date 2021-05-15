package ca.grindforloot.server.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
		
		return createEntityObject(key, new Document(), true, new HashSet<>());
	}
	
	/**
	 * Builds an Entity object from a document. No projections
	 * @param key
	 * @param doc
	 * @return
	 */
	protected Entity createEntityObject(Key key, Document doc) {
		return createEntityObject(key, doc, false, new HashSet<>());
	}
	
	/**
	 * Builds an Entity object from a document with projections
	 * @param key
	 * @param doc
	 * @param projections
	 * @return
	 */
	protected Entity createEntityObject(Key key, Document doc, Set<String> projections) {
		return createEntityObject(key, doc, false, projections);
	}
	
	/**
	 * Reflectively instantiates an entity object, given its type. Will implode if the type doesnt exist
	 * I feel like I'm gonna regret doing this reflectively. Infact i feel like i'm gonna regret doing anything reflectively.
	 * @param key
	 * @param doc
	 * @param isNew
	 * @param projections
	 * @return
	 */
	private Entity createEntityObject(Key key, Document doc, Boolean isNew, Set<String> projections) {
		Object[] values = {this, doc, isNew, projections};
				
		try {
			return (Entity) Utils.instantiate("com.grindforloot.entities." + key.getType(), values);
		}
		catch(Exception e) {
			throw new RuntimeException("Entity type " + key.getType() + " does not exist!" + e.getStackTrace());
		}
	}
	
	/**
	 * Inserts a single entity into the DB
	 * @param ent
	 */
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
	
	public void deleteEntity(Entity ent) {		
		delete(ent.getKey());
	}
	public void delete(Key key) {
		MongoCollection<Document> col = db.getCollection(key.getType());
		
		deleteInternal(key, col);
	}
	
	public void deleteEntity(Iterable<Entity> entities) {
		List<Key> keys = getKeysFromEntities(entities);
		
		delete(keys);
	}
	/**
	 * Delete a set of entities by their key.
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
	
	/**
	 * Delete a key from the DB
	 * @param key
	 * @param col
	 */
	private void deleteInternal(Key key, MongoCollection<Document> col) {
		col.deleteOne(QueryService.getFilterForId(key.getId()));
	}
	
	public Entity getEntity(Key key) {
		
		List<Document> docs = fetchRawInternal(key.getType(), QueryService.getFilterForId(key.getId()), null);
		
		if(docs.size() != 1)
			throw new IllegalStateException("cant have multiple docs with the same identifier. delete this project.");
		
		return createEntityObject(key, docs.get(0));
		
	}
	
	/**
	 * returns a list of built entities, given a type and a bson filter.
	 * @param type
	 * @param filter
	 * @return
	 */
	protected List<Entity> fetchInternal(String type, Bson filter){
		return fetchInternal(type, filter, null);
	}
	
	/**
	 * Returns a list of built entities, given a type and a Bson Filter. Any group fetch runs through this method.
	 * We create the entities using this.
	 * @param type - the entity type we're fetching
	 * @param filter - the composed bson filter
	 * @param projections - a set of the fields we're projection. This can be null.
	 * @return
	 */
	protected List<Entity> fetchInternal(String type, Bson filter, Set<String> projections){
		
		Bson composedProj = QueryService.generateProjections(projections);
		
		List<Document> rawList = fetchRawInternal(type, filter, composedProj);
		List<Entity> result = new ArrayList<>();
		
		for(Document doc : rawList) {
			Key key = new Key(type, doc.getObjectId("_id"));
						
			result.add(createEntityObject(key, doc));
		}
		
		return result;
	}
	
	/**
	 * Fetches a raw list of documents from the given collection.
	 * @param collection - the type of entity
	 * @param filter - the composed Bson filters
	 * @param projections - the composed Bson projections
	 * @return
	 */
	private List<Document> fetchRawInternal(String collection, Bson filter, Bson projections){
		List<Document> result = new ArrayList<>();
		
		FindIterable<Document> dbResult = db.getCollection(collection).find(filter).projection(projections);
		
		for(Document doc : dbResult) 
			result.add(doc);
		
		return result;
	}
	
	
	//Below this point are helper methods for sorting entities and extracting information from a collection of entities
	
	/**
	 * Extracts a list of keys from a list of entities
	 * @param entities
	 * @return
	 */
	public List<Key> getKeysFromEntities(Iterable<Entity> entities){
		List<Key> result = new ArrayList<>();
		
		for(Entity ent : entities)
			result.add(ent.getKey());
		
		return result;
	}
	
	/**
	 * Extracts a list of documents from a list of entities
	 * @param entities
	 * @return
	 */
	protected List<Document> getRawEntities(Iterable<Entity> entities){
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

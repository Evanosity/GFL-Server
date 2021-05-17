package ca.grindforloot.server;

import java.util.HashMap;
import java.util.Map;

import ca.grindforloot.server.db.DBService;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;

/**
 * When Json is received by the socket, an instance of this object is created.
 * 
 * Helper methods for dealing with the socket
 *
 * @author Evan
 *
 */
public class Context {
	private Map<String, Object> attributes;
	public final NetSocket socket;
	public final DBService db;
	public final Vertx vertx;
	
	private final JsonObject incoming;
	
	/**
	 * @param socket
	 * @param db
	 * @param incoming
	 */
	public Context(Vertx vertx, NetSocket socket, DBService db, JsonObject incoming) {
		if(socket == null)
			throw new IllegalArgumentException("Socket cannot be null.");
		this.socket = socket;
		
		if(db == null)
			throw new IllegalArgumentException("DB cannot be null");
		this.db = db;
		
		if(incoming == null)
			throw new IllegalArgumentException("Incoming cannot be null");
		this.incoming = incoming;
		
		if(vertx == null)
			throw new IllegalArgumentException("Do I even need these checks?");
		this.vertx = vertx;
	}
	
	public DBService getDB() {
		return db;
	}
	
	public String getStringProperty(String key) {
		return incoming.getString(key);
	}
	
	public Object getProperty(String key) {
		return incoming.getValue(key);
	}
	
	/**
	 * Push a JsonObject down the socket
	 * @param json
	 */
	public void writeToSocket(JsonObject json) {
		socket.write(Json.encodeToBuffer(json));
	}
	
	
	
	
	public void setAttribute(String key, Object obj) {
		if(attributes == null)
			attributes = new HashMap<>();
		
		attributes.put(key, obj);
	}
	
	public Object getAttribute(String key) {
		if(attributes == null)
			return null;
		
		return attributes.get(key);
	}
	
	/**
	 * Remove an attribute.
	 * @param key
	 * @return if the attribute existed before it was removed
	 */
	public boolean removeAttribute(String key) {
		if(attributes == null)
			return false;
		
		return attributes.remove(key) != null;
	}
}

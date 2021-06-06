package ca.grindforloot.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ca.grindforloot.server.db.DBService;
import ca.grindforloot.server.entities.Being;
import ca.grindforloot.server.entities.Session;
import ca.grindforloot.server.entities.User;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;

/**
 * When Json is received by the socket, an instance of this object is created.
 * It's then used to help manage the state of the user through the request. All methods pertaining to the scope of the request exist here.
 * 
 * 
 * Helper methods for dealing with the socket
 *
 * @author Evan
 *
 */
public class GameContext {
	private Map<String, Object> attributes;
	public final NetSocket socket;
	public final DBService db;
	public final Vertx vertx;
	public final Session session;
	public final String id;
	
	private final JsonObject incoming;
	
	/**
	 * This creates a GameContext object with no identifier; IE, this is not a request chat should be be replied to.
	 * @param vertx - the vertx instance for the server
	 * @param socket - the socket the message was received on
	 * @param db - the DBService object for this request
	 * @param incoming - the JsonObject that was received by the socket
	 * @param session - the session associated with this requested
	 */
	public GameContext(Vertx vertx, NetSocket socket, DBService db, JsonObject incoming, Session session) {
		this(vertx, socket, db, incoming, session, null);
	}
	
	/**
	 * 
	 * @param vertx - the vertx instance for the server
	 * @param socket - the socket the message was received on
	 * @param db - the DBService object for this request
	 * @param incoming - the JsonObject that was received by the socket
	 * @param session - the session associated with this requested
	 * @param id - the unique identifier for this request, so that it can be "replied" to by including the ID. This can be null.
	 */
	public GameContext(Vertx vertx, NetSocket socket, DBService db, JsonObject incoming, Session session, String id) {
		this.socket = socket;
		this.db = db;
		this.incoming = incoming;
		this.vertx = vertx;
		this.session = session;
		this.id = id;
	}
	
	public User getUser() {
		return session.getUser();
	}
	
	public Being getActiveBeing() {
		return getUser().getActiveBeing();
	}
	
	public DBService getDB() {
		return db;
	}
	
	/**
	 * Get a string from the incoming JsonObject
	 * @param key
	 * @return
	 */
	public String getStringProperty(String key) {
		return incoming.getString(key);
	}
	
	/**
	 * Get a property from the incoming JsonObject
	 * @param key
	 * @return
	 */
	public Object getProperty(String key) {
		return incoming.getValue(key);
	}
	
	/**
	 * Generates and then registers the appropriate EventBus handlers given the user's game state.
	 * For example; every time the user moves to a new "State", their location chat will swap.
	 * This method also unregisters all of the current handlers
	 * @param current - the current set of handlers, to be unregistered.
	 * @return
	 */
	public List<MessageConsumer<Object>> replaceStateHandlers(List<MessageConsumer<Object>> current) {
		
		//If the user IS NOT authenticated, we register no handlers.
		if(session.isAuthenticated() == false) {
			return new ArrayList<>(); //TODO do we replace this w `return current;`
		}
		
		for(MessageConsumer<Object> mc : current)
			unregisterConsumer(mc);
		
		List<MessageConsumer<Object>> result = new ArrayList<>();
		
		//Generate all the state-based handlers for this session.
		Map<String, Handler<Message<Object>>> handlers = generateStateHandlers();
		
		for(Entry<String, Handler<Message<Object>>> entry : handlers.entrySet()) {
			String address = entry.getKey();
			//register the new handler
			result.add(vertx.eventBus().consumer(address, entry.getValue()));
		}
		
		return result;
	}
	
	
	/**
	 * Generate an Address-Handler map of all the consumers for this character.
	 * This should really be in a different service.
	 * @return
	 */
	private Map<String, Handler<Message<Object>>> generateStateHandlers(){
		Map<String, Handler<Message<Object>>> result = new HashMap<>();
		
		//register their client for all the listeners necessary for each client
		//Anything that every client needs to listen to; essentially just chat.
		//also.... page updates. Register a handler to update.<sessionID>
		//and then write a method that infers a session ID from a character. char -> user -> session
		
		
		/**
		 * I need to think about this. Many of the chat channels will be state dependant.
		 * I'm thinking that we will give each socket a "chat.out" consumer, and then validate the message in that handler.
		 * 
		 * A formatted message has been received. Pipe it to the client.
		 * We will need multiple of these per chat channel. Global can just get set, but guild/party/etc
		 */
		result.put("chat.out", handler -> {
			JsonObject message = (JsonObject) handler.body(); //TODO codec
			
			JsonObject outgoing = new JsonObject();
			outgoing.put("type", "chat");
			outgoing.put("message", message.getString("message"));
			outgoing.put("sender", message.getString("sender"));
			outgoing.put("channel", message.getString("channel"));
						
			socket.write(Json.encodeToBuffer(outgoing));
		});

		
		//Generic client update
		result.put("update." + "",handler -> {//todo inject session id here
			JsonObject outgoing = (JsonObject) handler.body();
			
			socket.write(Json.encodeToBuffer(outgoing));
			
		});
		
		return result;
	}
	
	/**
	 * 
	 * Unregisters a consumer from the event bus. Automatically retries to ensure the consumer gets cleaned up.
	 * Why would it fail? Who knows, and I ain't finding out.
	 * @param mc
	 */
	private void unregisterConsumer(MessageConsumer<Object> mc) {
		mc.unregister(result -> {
			if(result.succeeded())
				return;
			else
				vertx.setTimer(5000, id -> unregisterConsumer(mc));
		});
		
	}
	
	/**
	 * Reply to this action.
	 * @param data
	 */
	public void reply(JsonObject data) {
		if(id == null || id.equals(""))
			throw new RuntimeException("Cannot reply to this request.");
		
		JsonObject reply = new JsonObject();
		reply.put("type", "reply");
		reply.put("id", id); //todo
		reply.put("data", data);
		
		writeToSocket(reply);
	}
	
	/**
	 * Push a JsonObject down the socket
	 * @param json
	 */
	public void writeToSocket(JsonObject json) {
		socket.write(Json.encodeToBuffer(json));
	}
	
	
	
	
	/**
	 * Set an attribute that is scoped to this object
	 * @param key
	 * @param obj
	 */
	public void setAttribute(String key, Object obj) {
		if(attributes == null)
			attributes = new HashMap<>();
		
		attributes.put(key, obj);
	}
	
	/**
	 * Get an attribute that is scoped to this object
	 * @param key
	 * @return
	 */
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

package ca.grindforloot.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.types.ObjectId;

import com.mongodb.client.MongoClient;

import ca.grindforloot.server.actions.Action;
import ca.grindforloot.server.actions.Login;
import ca.grindforloot.server.actions.Signup;
import ca.grindforloot.server.db.DBService;
import ca.grindforloot.server.db.Key;
import ca.grindforloot.server.entities.EntityService;
import ca.grindforloot.server.entities.Session;
import ca.grindforloot.server.errors.UserError;
import ca.grindforloot.server.services.ChatService;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;

public class MainVerticle extends AbstractVerticle{
	
	protected static MongoClient client = null;
	
	public static void main(String[]args) {
		
		Vertx vertx = Vertx.vertx();
		
		//client = MongoClients.create("");
		
		EventBus eb = vertx.eventBus();
		
		//TODO figure out codecs
		eb.registerDefaultCodec(JsonObject.class, null);
		
		vertx.deployVerticle(new MainVerticle(), id -> {
			
		});
		
		System.out.println(new ObjectId());
		
	}
	
	public void start() {	
		NetServer server = vertx.createNetServer();
		//connection is made to a client
		server.connectHandler(socket -> {
			
			List<MessageConsumer<Object>> consumers = new ArrayList<>();
			
			//THIS IS THE SCOPE OF EACH SESSION!
			
			//the GC will collect this object, and we will generate a new one in the scope of each request.
			DBService dbTemp = new DBService(client);
			
			Session newSession = new EntityService(dbTemp).createEntity("Session");			
			dbTemp.put(newSession);
			
			Key sessionKey = newSession.getKey();
			
			socket.handler(buffer -> {		
				//This is the scope of each individual request.
				JsonObject incoming = buffer.toJsonObject();
				
				DBService db = new DBService(client);
				
				Session session = db.getEntity(sessionKey);
				
				//Compose the context object. This gets passed to every action and service.
				GameContext ctx = new GameContext(vertx, socket, db, incoming, session);
				
				//we process the user's request
				try {
					ChatService cs = new ChatService(ctx);
					
					//The "type" of the request describes, vaguely, what the client is trying to do.
					switch(ctx.getStringProperty("type")) {
					//in the case of an action, execute server-side logic.
					case "action":
						Action action = generateAction(ctx.getStringProperty("action"), ctx);
						
						action.doChecks();
						
						action.perform();
						
						//rid ourselves of the old consumers.
						consumers.removeIf(consumer -> {
							unregisterConsumer(consumer);
							return consumer != null;
						});
						
						//generate the new consumers.
						consumers.addAll(registerStateHandlers(cs, socket));
						
						break;
						
					//The user is sending a chat message
					case "chat":
						String channel = ctx.getStringProperty("channel");
						String message = ctx.getStringProperty("message");
						
						cs.sendMessage(channel, message);
						
						break;
					default:
						throw new UserError("Internal Error", "Request type " + ctx.getStringProperty("type") + " is unsupported.");
					}
				}
				//UserError gets caught and then displayed back to the user.
				catch(UserError e) {
					JsonObject error = new JsonObject();
					
					error.put("type", "error");
					error.put("title", e.getErrorName());
					error.put("message", e.getMessage());
					
					if(e.getCause() != null) 
						error.put("cause", e.getCause().getStackTrace().toString()); //todo lol
					
					ctx.writeToSocket(error);
				}

				
				System.out.println(incoming.getString("action"));
				
				JsonObject outgoing = new JsonObject();
				outgoing.put("message", "Hey!");
				
				socket.write(Json.encodeToBuffer(outgoing));		
			});
			
			/**
			 * Whenever a user disconnects, this handler get called.
			 * We unregister all of their handlers, so the event bus doesn't bloat.
			 * We also delete their session.
			 */
			socket.closeHandler(handler -> {				
				for(MessageConsumer<Object> mc : consumers)
					unregisterConsumer(mc);
				
				DBService scopeDB = new DBService(client);
				
				scopeDB.doTransaction(() -> {
					scopeDB.delete(sessionKey);
				});
			});
		});
		
		//TODO cron jobs? Decide if I'll make an XML file or do them all programatically.
		
		//TODO environment variables
		server.listen(8080, "0.0.0.0", res -> {
			if(res.succeeded())
				System.out.println("Server successfully started");
			else
				System.out.println("Server failed to start");
		});
	}
	
	/**
	 * Generates and then registers the appropriate EventBus handlers given the user's game state.
	 * For example; every time the user moves to a new "State", their location chat will swap.
	 * @param cs
	 * @param socket
	 * @return
	 */
	public List<MessageConsumer<Object>> registerStateHandlers(ChatService cs, NetSocket socket) {
		
		//If the user IS NOT authenticated, we register no handlers.
		if(cs.ctx.session.isAuthenticated() == false) {
			return new ArrayList<>();
		}
		
		List<MessageConsumer<Object>> result = new ArrayList<>();
		
		//Generate all the state-based handlers for this session.
		Map<String, Handler<Message<Object>>> handlers = cs.generateStateHandlers(socket);
		
		for(Entry<String, Handler<Message<Object>>> entry : handlers.entrySet()) {
			String address = entry.getKey();
			//register the new handlers.
			result.add(vertx.eventBus().consumer(address, entry.getValue()));
		}
		
		return result;
	}
	
	/**
	 * Generate an action.
	 * @param <A>
	 * @param actionName
	 * @param ctx
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <A extends Action> A generateAction(String actionName, GameContext ctx) {
		switch(actionName) {
		case "login":
			return (A) new Login(ctx);
		case "signup":
			return (A) new Signup(ctx);
		default:
			throw new IllegalArgumentException("Action " + actionName + " not supported by generateAction()");
		}
	}
		
	/**
	 * 
	 * Unregisters a consumer from the event bus. Automatically retries to ensure the consumer gets cleaned up.
	 * Why would it fail? Who knows, and I ain't finding out.
	 * @param mc
	 */
	public void unregisterConsumer(MessageConsumer<Object> mc) {
		mc.unregister(result -> {
			if(result.succeeded())
				return;
			else
				//in the event of a looping failure, we dont want to bog down this thread.
				//this is a textbook bandaid fix
				vertx.setTimer(5000, handler -> {
					unregisterConsumer(mc);
				});
		});
		
	}
}

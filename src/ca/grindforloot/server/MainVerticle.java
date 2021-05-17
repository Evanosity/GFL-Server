package ca.grindforloot.server;

import java.util.ArrayList;
import java.util.List;

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

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetServer;

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
			
			//THIS IS THE SCOPE OF EACH SESSION!
			
			DBService db = new DBService(client);
			
			Session newSession = new EntityService(db).createEntity("Session");			
			db.put(newSession);
			
			Key sessionKey = newSession.getKey();
			
			socket.handler(buffer -> {		
				//This is the scope of each individual request.
				JsonObject incoming = buffer.toJsonObject();
				
				Session session = db.getEntity(sessionKey);
				
				//Compose the context object. This gets passed to every action and service.
				Context ctx = new Context(vertx, socket, db, incoming, session);
				
				//we process the user's request
				try {
					
					//The "type" of the request describes, vaguely, what the client is trying to do.
					switch(ctx.getStringProperty("type")) {
					//in the case of an action, execute server-side logic.
					case "action":
						Action action = generateAction(ctx.getStringProperty("action"), ctx);
						
						action.doChecks();
						
						action.perform();
						
						break;
						
					//The user is sending a chat message
					case "chat":
						String channel = ctx.getStringProperty("channel");
						String message = ctx.getStringProperty("message");
						
						vertx.eventBus().publish("chat.out." + channel, message);
						
						break;
					default:
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
			
			//register their client for all the listeners necessary for each client
			//Anything that every client needs to listen to; essentially just chat.
			//also.... page updates. Register a handler to update.<sessionID>
			//and then write a method that infers a session ID from a character. char -> user -> session
			
			List<MessageConsumer<Object>> consumers = new ArrayList<>();
			
			consumers.add(vertx.eventBus().consumer("chat.out", handler -> {
				//chat message
			}));
			
			//Client update
			consumers.add(vertx.eventBus().consumer("update." + newSession.getId(), handler -> {
				JsonObject outgoing = (JsonObject) handler.body();
				
				socket.write(Json.encodeToBuffer(outgoing));
				
			}));
			
			/**
			 * Whenever a user disconnects, this handlers get called.
			 * We unregister all of their handlers, so the event bus doesn't bloat.
			 * We also delete their session.
			 */
			socket.closeHandler(handler -> {				
				for(MessageConsumer<Object> mc : consumers)
					unregisterConsumer(mc);
				
				db.doTransaction(() -> {
					db.delete(sessionKey);
				});
				
				//
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
	 * Generate an action.
	 * @param <A>
	 * @param actionName
	 * @param ctx
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <A extends Action> A generateAction(String actionName, Context ctx) {
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

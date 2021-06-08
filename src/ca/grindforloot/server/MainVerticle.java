package ca.grindforloot.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mongodb.client.MongoClient;

import ca.grindforloot.server.actions.Action;
import ca.grindforloot.server.actions.Login;
import ca.grindforloot.server.actions.Signup;
import ca.grindforloot.server.controllers.Controller;
import ca.grindforloot.server.db.DBService;
import ca.grindforloot.server.db.Key;
import ca.grindforloot.server.db.Query;
import ca.grindforloot.server.db.QueryService;
import ca.grindforloot.server.db.QueryService.FilterOperator;
import ca.grindforloot.server.entities.EntityService;
import ca.grindforloot.server.entities.Script;
import ca.grindforloot.server.entities.Session;
import ca.grindforloot.server.errors.UserError;
import ca.grindforloot.server.services.ChatService;
import ca.grindforloot.server.services.EventBusService;
import ca.grindforloot.server.services.ScriptService;
import ca.grindforloot.server.services.TimerService;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetServer;

public class MainVerticle extends AbstractVerticle{
	
	protected static MongoClient client = null;
	protected static Vertx vertx = null;
	
	/**
	 * A map of all active cron jobs.
	 * Key - the timer ID
	 * Value - the key of the script entity
	 */
	
	public static void main(String[]args) {
		
		vertx = Vertx.vertx();
		
		EventBus eb = vertx.eventBus();
				
		//TODO figure out codecs
		//eb.registerDefaultCodec(JsonObject.class, new JsonObjectMessageCodec());
		
		Map<String, Object> globalContext = new HashMap<>();
		globalContext.put("global", "Evan is the best - ");
		
		ScriptService.init(globalContext);
		TimerService.init(client, vertx);
		
		Handler<Long> scriptHandler = id -> {
			Map<String, Object> context = new HashMap<>();
			context.put("test", id);
			
			try {
				ScriptService.interpret("var x = new java.lang.Long(1); print(global + test + x);", context);
			}
			catch(Throwable e) {
				System.out.println(e.getMessage());
				System.out.println(e.getCause().getClass());
				vertx.close();
			}
		};
		vertx.setPeriodic(500, id -> {
			vertx.setTimer(1000, scriptHandler);
		});
		vertx.setPeriodic(1000, id -> {
			vertx.setTimer(750, scriptHandler);
		});

		/*
		client = MongoClients.create("");
		System.out.println(new ObjectId());
		*/
	}
	
	/**
	 * spin up the TCP server, and set its handlers accordingly.
	 */
	public void start() {	
		NetServer server = vertx.createNetServer();
		//connection is made to a client
		server.connectHandler(socket -> {
			
			//THIS IS THE SCOPE OF EACH SESSION!
			
			//This is all the EventBus consumers for this user. These gets cleaned up when the socket disconnects.
			//Chat, update, etc
			List<MessageConsumer<Object>> consumers = new ArrayList<>();
			
			
			//the GC will collect this object, and we will generate a new one in the scope of each request.
			DBService dbTemp = new DBService(client);
			
			Session newSession = new EntityService(dbTemp).createEntity("Session");			
			dbTemp.put(newSession);
			
			Key sessionKey = newSession.getKey();
			
			socket.handler(buffer -> {		
				//This is the scope of each individual request.
				
				JsonObject incoming = buffer.toJsonObject();
				String id = incoming.getString("identifier");
				
				DBService db = new DBService(client);
				
				/**
				 * the session info should come from the client. This will save memory on the server
				 */
				Session session = db.getEntity(sessionKey);
				
				//Compose the context object. This gets passed to every action and service.
				GameContext ctx = new GameContext(vertx, socket, db, incoming, session, id);
				
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
						
						//Generate the new eventbus consumers for this user
						ctx.replaceStateHandlers(consumers);
						
						break;
						
					//The user is sending a chat message
					case "chat":
						String channel = ctx.getStringProperty("channel");
						String message = ctx.getStringProperty("message");
						
						cs.sendMessage(channel, message);
						
						break;
						
					//the user is loading a view of something, and we need to generate the necessary information.
					//"Controllers". These need to be built in conjunction with JavaFX nodes.
					case "view":
						
						String viewName = ctx.getStringProperty("view");
						
						Controller controller = generateController(viewName, ctx);
						
						JsonObject result = controller.process();
						
						JsonObject outgoing = new JsonObject();
						
						outgoing.put("type", "view");
						outgoing.put("name", viewName);
						outgoing.put("data", result);
						
						ctx.writeToSocket(outgoing);
						
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
			 * We also delete their session. TODO do we reallllly need to do that?
			 */
			socket.closeHandler(handler -> {
				EventBusService service = new EventBusService(vertx);
				for(MessageConsumer<Object> mc : consumers)
					service.unregisterConsumer(mc);
				
				Key scopeSessionKey = null; //TODO infer their key, so there's no leftover memory. I think?
				//I need to put a lot of thought into session validation
				
				DBService scopeDB = new DBService(client);
				
				scopeDB.doTransaction(() -> {
					scopeDB.delete(scopeSessionKey);
				});
			});
		});
		
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
	 * @param <T>
	 * @param actionName
	 * @param ctx
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T extends Action> T generateAction(String actionName, GameContext ctx) {
		switch(actionName) {
		case "login":
			return (T) new Login(ctx);
		case "signup":
			return (T) new Signup(ctx);
		case "combat-escape":
		default:
			throw new IllegalArgumentException("Action " + actionName + " not supported by generateAction()");
		}
	}
	
	/**
	 * Generate a controller
	 * @param <T>
	 * @param controllerName
	 * @param ctx
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T extends Controller> T generateController(String controllerName, GameContext ctx) {
		switch(controllerName) {
		case "item":
			//return (T) new Controller(ctx);
		default:
			throw new IllegalArgumentException("Controller " + controllerName + " not supported by generateController()");
		}
	}
}

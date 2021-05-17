package ca.grindforloot.server;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import ca.grindforloot.server.db.DBService;
import ca.grindforloot.server.db.Key;
import ca.grindforloot.server.entities.EntityService;
import ca.grindforloot.server.entities.Session;

import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.dns.DnsClient;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetServer;

public class MainVerticle extends AbstractVerticle{
	
	protected static MongoClient client = null;
	
	public static void main(String[]args) {
		
		Vertx vertx = Vertx.vertx();
		
		//client = MongoClients.create("");

				
		DnsClient dns = vertx.createDnsClient(53, "9.9.9.9");
		
		dns.reverseLookup("9.9.9.9", ar -> {
			if(ar.succeeded()) {
				System.out.println(ar.result());
			}
		});
		
		EventBus eb = vertx.eventBus();
		
		//TODO
		eb.registerDefaultCodec(JsonObject.class, null);
		
		//vertx.deployVerticle(new MainVerticle());
		
		System.out.println(new ObjectId());
		
	}
	
	public void start() {	
		NetServer server = vertx.createNetServer();
		//connection comes in
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
				Context context = new Context(vertx, socket, db, incoming, session);
				


				
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
			
			//Game update
			consumers.add(vertx.eventBus().consumer("update." + session.getId(), handler -> {
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
		
		//TODO cron jobs?
		
		//TODO environment variables
		server.listen(8080, "0.0.0.0", res -> {
			if(res.succeeded())
				System.out.println("Server successfully started");
			else
				System.out.println("Server failed to start");
		});
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
				unregisterConsumer(mc);
		});
		
	}
}

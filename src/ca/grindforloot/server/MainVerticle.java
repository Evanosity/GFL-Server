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
	
	public static void main(String[]args) {
		
		Vertx vertx = Vertx.vertx();
		
		//MongoClient client = MongoClients.create("");
		MongoClient client = null;
				
		DnsClient dns = vertx.createDnsClient(53, "9.9.9.9");
		
		dns.reverseLookup("9.9.9.9", ar -> {
			if(ar.succeeded()) {
				System.out.println(ar.result());
			}
		});
		
		EventBus eb = vertx.eventBus();
		
		//vertx.deployVerticle(new MainVerticle());
		
		System.out.println(new ObjectId());
		
	}
	
	public void start() {	
		NetServer server = vertx.createNetServer();
		//connection comes in
		server.connectHandler(socket -> {
			
			//THIS IS THE SCOPE OF EACH SESSION!
			
			JsonObject result = new JsonObject();
			result.put("clicked", "no");
			
			DBService db = new DBService(null);
			
			Session session = new EntityService(db).createEntity("Session");			
			db.put(session);
			
			Key sessionKey = session.getKey();
			
			socket.handler(buffer -> {		
				//This is the scope of each individual request.
				
				JsonObject incoming = buffer.toJsonObject();
				
				Context context = new Context(vertx, socket, db, incoming);

				
				System.out.println(incoming.getString("action"));
				
				JsonObject outgoing = new JsonObject();
				outgoing.put("message", "Hey!");
				
				result.put("clicked", "yes");
				
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
			
			socket.closeHandler(handler -> {
				System.out.println(result.getString("clicked"));
				
				for(MessageConsumer<Object> mc : consumers)
					unregisterConsumer(mc);
				
				db.delete(sessionKey);
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

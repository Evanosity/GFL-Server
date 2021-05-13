package ca.grindforloot.server;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.bson.types.ObjectId;

import com.mongodb.client.MongoClient;

import ca.grindforloot.server.actions.Action;
import ca.grindforloot.server.errors.UserError;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;

public class MainVerticle extends GFLVerticle{
	
	public static void main(String[]args) {
		
		Vertx vertx = Vertx.vertx();
		
		//MongoClient client = MongoClients.create("");
		MongoClient client = null;
		
		Environment env = new Environment();
		
		
		vertx.deployVerticle(new MainVerticle(env));
		
		System.out.println(new ObjectId());
		
	}
	
	public MainVerticle(Environment env) {
		super(env);
	}
	
	public void start() {	
		NetServer server = vertx.createNetServer();
		//connection comes in
		server.connectHandler(socket -> {
			
			//we add a handler for the socket.
			socket.handler(buffer -> {		
				JsonObject incoming = buffer.toJsonObject();
				
				String actionName = incoming.getString("action");
								
				try {
					Class<?> clazz = Class.forName("ca.grindforloot.server.actions." + actionName);
					Constructor<?> cons = clazz.getConstructor(NetSocket.class, JsonObject.class);
					
					Action action = (Action) cons.newInstance(socket, incoming);
					
					try {
						action.perform();
					}
					catch(UserError e){
						
					}
					
				} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					//TODO log it.
					throw new RuntimeException("Attempted to instantiate action that does not exist");
				}
			});
		});
		
		server.listen(8080, "0.0.0.0", res -> {
			if(res.succeeded())
				System.out.println("Server successfully started");
			else
				System.out.println("Server failed to start");
		});
	}
}

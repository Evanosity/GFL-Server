package ca.grindforloot.server;

import org.bson.types.ObjectId;

import com.mongodb.client.MongoClient;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetServer;

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
			
			socket.handler(buffer -> {		
				JsonObject incoming = buffer.toJsonObject();
				
				String actionName = incoming.getString("action");
							
				Utils.instantiate("ca.grindforloot.server.actions." + actionName, socket, incoming);
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

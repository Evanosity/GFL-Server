package ca.grindforloot.server.services;

import java.util.HashMap;
import java.util.Map;

import ca.grindforloot.server.GameContext;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetSocket;

public class ChatService extends Service{

	public ChatService(GameContext ctx) {
		super(ctx);
	}
	
	public void sendMessage(String channel, String message) {
		
		String senderName = ctx.getActiveBeing().getName();
		
		JsonObject formattedMessage = new JsonObject();
		formattedMessage.put("channel", channel);
		formattedMessage.put("message", message);
		formattedMessage.put("sender", senderName);
		
		vertx.eventBus().publish("chat.out", message);
	}
	
	/**
	 * Generate an Address-Handler map of all the consumers for this character.
	 * This should really be in a different service.
	 * @return
	 */
	public Map<String, Handler<Message<Object>>> generateStateHandlers(NetSocket output){
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
		 */
		result.put("chat.out", handler -> {
			JsonObject message = (JsonObject) handler.body(); //TODO codec
			
			JsonObject outgoing = new JsonObject();
			outgoing.put("type", "chat");
			outgoing.put("message", message.getString("message"));
			outgoing.put("sender", message.getString("sender"));
			outgoing.put("channel", message.getString("channel"));
			
			//We then need to evaluate if this message should actually be retrieved. CRIPES this is gonna be inefficient.
			
			socket.write(Json.encodeToBuffer(outgoing));
		});

		
		//Generic client update
		result.put("update." + "",handler -> {//todo inject session id here
			JsonObject outgoing = (JsonObject) handler.body();
			
			socket.write(Json.encodeToBuffer(outgoing));
			
		});
		
		return result;
	}
	
	public String formatChatMessage(String message) {
		String newMessage = message;
		
		//apply a regex to the message
		
		return newMessage;
	}
	
	/**
	 * 
	 * @param channel
	 * @param output
	 * @return
	 */
	public Handler<Message<Object>> generateChatHandler(NetSocket output){
		return new Handler<Message<Object>>() {
			@Override
			public void handle(Message<Object> event) {
				JsonObject message = (JsonObject) event.body(); //TODO codec
				
				JsonObject outgoing = new JsonObject();
				outgoing.put("type", "chat");
				outgoing.put("message", message.getString("message"));
				outgoing.put("sender", message.getString("sender"));
				
				//We then need to evaluate if this message should actually be retrieved. CRIPES this is gonna be inefficient.
				output.write(Json.encodeToBuffer(outgoing));
			}
		};
	}
}

package ca.grindforloot.server.services;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;

/**
 * Helper methods for the vert.x event bus.
 * 
 * This has basically nothing rn but I bit the bullet and created it.
 * 
 * @author Evan
 *
 */
public class EventBusService {
	private final Vertx vertx;
	public EventBusService(Vertx v) {
		vertx = v;
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
				vertx.setTimer(5000, id -> unregisterConsumer(mc));
		});
	}
}

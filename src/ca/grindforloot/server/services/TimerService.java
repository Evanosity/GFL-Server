package ca.grindforloot.server.services;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.mongodb.client.MongoClient;

import ca.grindforloot.server.db.DBService;
import ca.grindforloot.server.db.Key;
import ca.grindforloot.server.db.Query;
import ca.grindforloot.server.db.QueryService;
import ca.grindforloot.server.db.QueryService.FilterOperator;
import ca.grindforloot.server.entities.Script;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

/**
 * Service for dealing with Vert.x timers and periodics, including cron jobs.
 * @author Evan
 *
 */
public class TimerService {
	private static TimerService instance = null;
	public static TimerService getInstance() {
		if(instance == null)
			throw new IllegalStateException("TimerSerivce not intialized");
		
		return instance;
	}
	
	public static TimerService init(MongoClient client, Vertx vertx) {
		if(instance != null)
			throw new IllegalStateException("TimerService already initialized");
		
		instance = new TimerService(client, vertx);
		
		return instance;
	}
	
	//All of the active repeating timers. This should probably be converted to being stored in
	//redis
	private Map<Long, AtomicInteger> repeatingTimers = new ConcurrentHashMap<>();
	
	//all of the active cron jobs
	private Map<Long, Key> cronJobs = new ConcurrentHashMap<>();
	private final Vertx vertx;
	private final MongoClient client;
	/**
	 * Initialize the service
	 * @param c - the mongoclient
	 * @param v - vertx
	 */
	private TimerService(MongoClient c, Vertx v) {
		
		this.vertx = v;
		this.client = c;
		
		DBService db = new DBService(client);
		QueryService qs = new QueryService(db);
		
		Query q = new Query("Script").addFilter("cron", FilterOperator.NOT_EQUAL, null);
		q.addProjections("_id", "cron");
		
		List<Script> cronScripts = qs.runEntityQuery(q);
		
		//Set timers for ever cron job.
		for(Script s : cronScripts) {
			Key key = s.getKey();
			
			Long periodicId = vertx.setPeriodic(s.getCronTimer(), getCronHandler(vertx));
			
			cronJobs.put(periodicId, key);
		}
		
		//Once every hour, discover new cron jobs.
		vertx.setPeriodic(3600000, id -> {
			DBService scopeDB = new DBService(client);
			QueryService scopeQs = new QueryService(scopeDB);
			
			Query query = new Query("Script").addFilter("cron", FilterOperator.NOT_EQUAL, null);
			q.addProjections("_id", "cron");
			
			List<Script> scripts = scopeQs.runEntityQuery(query);
			
			for(Script sc : scripts) {
				if(cronJobs.containsValue(sc.getKey()))
					continue;
				
				//If it does not already exist, register it.
				Long timerId = vertx.setPeriodic(sc.getCronTimer(), getCronHandler(vertx));
				
				cronJobs.put(timerId, sc.getKey());
			}
		});
	}
	
	/**
	 * Set a one-shot timer
	 * @param handler
	 * @param timer
	 * @return
	 */
	public Long addTimer(Handler<Long> handler, Long timer) {
		return vertx.setTimer(timer, handler);
	}
	
	/**
	 * Set a repeating timer
	 * @param handler - the actual logic to be executed
	 * @param delay - the timer delay
	 * @param repeats - The number of repeats to do
	 * @return
	 */
	public Long addRepeatingTimer(Handler<Long> handler, Long delay, Long repeats) {
		Long identifier = vertx.setTimer(delay, getRepeatingTimerHandler(handler, delay, repeats));
		
		AtomicInteger counter = new AtomicInteger();
		counter.set(repeats.intValue());
		
		repeatingTimers.put(identifier, counter);

		return identifier;
	}
	
	/**
	 * Build a handler for repeating timers
	 * @param handler
	 * @param delay
	 * @param repeats
	 * @return
	 */
	private Handler<Long> getRepeatingTimerHandler(Handler<Long> handler, Long delay, Long repeats){
		return id -> {
			handler.handle(id);
			
			if(repeatingTimers.get(id).decrementAndGet() == 0)
				repeatingTimers.remove(id);
			else
				vertx.setTimer(delay, getRepeatingTimerHandler(handler, delay, repeats));
		};
	}
	
	/**
	 * Generate the cron job handler. This handles the execution of each cron job
	 * @param vertx
	 * @return
	 */
	private Handler<Long> getCronHandler(Vertx vertx){
		return id -> vertx.executeBlocking(promise -> {
			
			DBService db = new DBService(client);
			
			Script script = db.getEntity(cronJobs.get(id));
			
			try {
				//If the cron job gets disabled via DB, don't execute it.
				if(script.getCronTimer() == 0) {
					
					vertx.cancelTimer(id);
					cronJobs.remove(id);
					
					promise.fail("Cron job cancelled.");
				}
				else {
					//TODO build the context
					ScriptService.interpret(script, null);
					
					promise.complete();
				}
			}
			catch(Throwable e) {
				promise.fail("Interrupted during cron job id" + id + " " + script.getName() + ": " + e.toString());
			}
			
		}, false);
	}
}

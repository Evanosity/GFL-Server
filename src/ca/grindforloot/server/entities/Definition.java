package ca.grindforloot.server.entities;

import java.util.Set;

import org.bson.Document;

import ca.grindforloot.server.db.DBService;
import ca.grindforloot.server.db.Entity;

/**
 * 
 * @author Evan
 *
 * @param <T> the entity type for this definition
 */
public abstract class Definition <T extends Entity> extends Entity{
	
	protected Definition(DBService db, Document raw, boolean isNew, Set<String> projections) {
		super(db, raw, isNew, projections);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Generate something from this definition. Item, Monster, whatever.
	 * @return
	 */
	public abstract T instantiate();
}

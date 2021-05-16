package ca.grindforloot.server.entities;

import java.util.Set;

import org.bson.Document;

import ca.grindforloot.server.db.DBService;
import ca.grindforloot.server.db.Entity;

/**
 * 
 * @author Evan
 *
 * @param <T> the entity type we're generating with generate()
 */
public abstract class Generator <T extends Entity> extends Entity{

	protected final Definition<T> def;
	
	protected Generator(Definition<T> def, DBService db, Document raw, boolean isNew, Set<String> projections) {
		super(db, raw, isNew, projections);
		
		this.def = def;
	}
	
	public abstract T generate();
}

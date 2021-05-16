package ca.grindforloot.server.entities;

import java.util.Set;

import org.bson.Document;

import ca.grindforloot.server.db.DBService;

public class ItemDefinition extends Definition<Item>{

	protected ItemDefinition(DBService db, Document raw, boolean isNew, Set<String> projections) {
		super(db, raw, isNew, projections);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return "ItemDefinition";
	}

	@Override
	public Item instantiate() {
		Item item = new EntityService(db).createEntity("Item");
		
		
		
		return item;
	}

}

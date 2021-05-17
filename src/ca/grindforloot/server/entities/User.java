package ca.grindforloot.server.entities;

import java.util.List;
import java.util.Set;

import org.bson.Document;

import ca.grindforloot.server.db.DBService;
import ca.grindforloot.server.db.Entity;
import ca.grindforloot.server.db.Key;

public class User extends Entity{
	
	protected User(DBService db, Document doc, boolean isNew, Set<String> projections) {
		super(db, doc, isNew, projections);
	}
	
	/**
	 * Get the users email
	 * @return
	 */
	public String getEmail() {
		return raw.getString("email");
	}
	
	/**
	 * Set the users email
	 * @param email
	 */
	public void setEmail(String email) {
		raw.put("email", email);
	}
	
	/**
	 * Returns true if the given string is equal to the hashed password.
	 * @param hash
	 * @return
	 */
	public boolean validatePassword(String hash) {
		return hash.equals(raw.getString("password"));
	}
	
	/**
	 * Replace the hashed password
	 * @param hash
	 * @return
	 */
	public boolean setPassword(String hash) {
		String oldPass = raw.getString("password");
		
		raw.put("password", hash);
		
		return oldPass.equals(hash);
	}
	public List<Key> getCharacterKeys(){
		return null;
	}

	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return "User";
	}
}

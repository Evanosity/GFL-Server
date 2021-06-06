package ca.grindforloot.server.actions;

import ca.grindforloot.server.GameContext;
import ca.grindforloot.server.Request;
import ca.grindforloot.server.errors.UserError;

public abstract class Action extends Request{
	
	public Action(GameContext ctx) {
		super(ctx);
	}
	/**
	 * Given the request and response, do something.
	 */
	public abstract void perform() throws UserError;
	
	/**
	 * Checks to see if the user is authenticated before allowing the action to proceed.
	 * 
	 * Override this method is this is an action where the user <i>doesn't</i> have to be authenticated(login, signup)
	 * 
	 * @throws UserError
	 */
	public void doChecks() throws UserError{
		if(ctx.session.isAuthenticated() == false)
			throw new UserError("Authentication Error", "You have to be logged in to do this.");
	}
}

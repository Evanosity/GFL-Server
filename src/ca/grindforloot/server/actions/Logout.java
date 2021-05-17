package ca.grindforloot.server.actions;

import ca.grindforloot.server.GameContext;
import ca.grindforloot.server.errors.UserError;

public class Logout extends Action{

	public Logout(GameContext context) {
		super(context);
	}

	@Override
	public void perform() throws UserError {
		ctx.session.logout();
		
		db.put(ctx.session);
	}
}

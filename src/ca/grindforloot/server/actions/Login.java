package ca.grindforloot.server.actions;

import ca.grindforloot.server.Context;
import ca.grindforloot.server.errors.UserError;

public class Login extends Action{

	public Login(Context context) {
		super(context);
	}

	@Override
	public void perform() throws UserError {
		// TODO session management
		
	}

}

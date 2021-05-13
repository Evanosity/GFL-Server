package ca.grindforloot.server.errors;

public class UserError extends Throwable{
	private static final long serialVersionUID = 0L;
	
	private final String message;
	
	public UserError(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}

}

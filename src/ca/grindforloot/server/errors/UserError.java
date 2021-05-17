package ca.grindforloot.server.errors;

public class UserError extends Throwable{
	private static final long serialVersionUID = 0L;
	
	private final String message;
	private final String errorName;
	private final Throwable cause;
	
	public UserError(String errorName, String message) {
		this(errorName, message, null);
	}
	
	public UserError(String errorName, String message, Throwable cause) {
		this.message = message;
		this.errorName = errorName;
		this.cause = cause;
	}
	
	public String getMessage() {
		return message;
	}
	public String getErrorName() {
		return errorName;
	}
	public Throwable getCause() {
		return cause;
	}

}

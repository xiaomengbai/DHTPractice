public class UserNotExistException extends Exception{
    public UserNotExistException() { super(); }
    public UserNotExistException(String message) { super(message); }
    public UserNotExistException(String message, Throwable cause) { super(message, cause); }
    public UserNotExistException(Throwable cause) { super(cause); }
}
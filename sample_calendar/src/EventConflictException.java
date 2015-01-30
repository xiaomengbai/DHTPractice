public class EventConflictException extends Exception{
    public EventConflictException() { super(); }
    public EventConflictException(String message) { super(message); }
    public EventConflictException(String message, Throwable cause) { super(message, cause); }
    public EventConflictException(Throwable cause) { super(cause); }
}
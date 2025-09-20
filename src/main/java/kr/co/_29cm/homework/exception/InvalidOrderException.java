package kr.co._29cm.homework.exception;

public class InvalidOrderException extends BusinessException {
    
    public InvalidOrderException(String message) {
        super("INVALID_ORDER", message);
    }

    public InvalidOrderException(String message, Throwable cause) {
        super("INVALID_ORDER", message, cause);
    }
}

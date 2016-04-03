package parser;


public class InvalidMessageFormatException extends Exception {

    public InvalidMessageFormatException(String message) {
        super(message);
    }

    public InvalidMessageFormatException(String message, Throwable e) {
        super(message, e);
    }

}

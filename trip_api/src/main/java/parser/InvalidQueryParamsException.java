package parser;


public class InvalidQueryParamsException extends Exception {

    public InvalidQueryParamsException(String message, Exception ex) {
        super(message, ex);
    }

    public InvalidQueryParamsException(String message) {
        super(message);
    }

}

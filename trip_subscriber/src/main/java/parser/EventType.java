package parser;


import com.fasterxml.jackson.annotation.JsonCreator;

public enum EventType {
    BEGIN, UPDATE, END;

    @JsonCreator
    public static EventType fromString(String event) {
        return EventType.valueOf(event.toUpperCase());
    }

}

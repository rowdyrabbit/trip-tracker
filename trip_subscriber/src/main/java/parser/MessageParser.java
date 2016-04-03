package parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;


public class MessageParser {

    private static final Logger logger = LoggerFactory.getLogger(MessageParser.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    public static Message parseMessage(String msg) throws InvalidMessageFormatException {
        try {
            Message message = mapper.readValue(msg, Message.class);
            validateMessage(message);
            return message;
        } catch (Exception ex) {
            logger.error(format("Error occurred during parsing of message: %s", msg), ex);
            throw new InvalidMessageFormatException(format("Error occurred during parsing of message: %s", msg), ex);
        }
    }

    private static void validateMessage(Message message) throws InvalidMessageFormatException {
        if (message.getEvent() == EventType.END && message.getFare() == null) {
            throw new InvalidMessageFormatException(format("Event type '%s' has missing fare value", message.getEvent()));
        }
        if (message.getEvent() != EventType.END && message.getFare() != null) {
            throw new InvalidMessageFormatException(
                    format("Event type '%s' has a fare value of '%.2f' when it should be null", message.getEvent(), message.getFare()));
        }

    }
}

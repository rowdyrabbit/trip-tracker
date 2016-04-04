package subscriber;


import db.MessagePersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import parser.InvalidMessageFormatException;
import parser.Message;
import parser.MessageParser;

public class MessageHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    private String message;
    private MessagePersistenceService service;

    public MessageHandler(String message, MessagePersistenceService service) {
        this.message = message;
        this.service = service;
    }

    @Override
    public void run() {
        try {
            Message msg = MessageParser.parseMessage(message);
            service.saveMessage(msg);
        } catch (InvalidMessageFormatException ex) {
            // instead of just logging the failure here, we could publish
            logger.error(String.format("Error occurred during parsing of message: %s", message), ex);
        }
    }

}

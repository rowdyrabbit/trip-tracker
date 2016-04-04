package subscriber;

import db.MessagePersistenceService;
import org.junit.Test;
import parser.Message;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


public class MessageHandlerTest {

    private final MessagePersistenceService mockService = mock(MessagePersistenceService.class);
    private final String msg = "{\"event\":\"update\",\"tripId\":432, \"lat\":37.79947, \"lng\":122.511635, \"epoch\":1392864673040}";
    private final String invalidMsg = "{\"event\":\"start\",\"tripId\":432, \"lat\":37.79947, \"lng\":122.511635, \"epoch\":1392864673040}";


    @Test
    public void shouldCallSaveOnPersistenceServiceIfMessageIsValid() throws Exception {
        MessageHandler handler = new MessageHandler(msg, mockService);
        handler.run();
        verify(mockService, times(1)).saveMessage(any(Message.class));
    }

    @Test
    public void shouldNotCallSaveOnPersistenceServiceIfMessageIsInValid() {
        MessageHandler handler = new MessageHandler(invalidMsg, mockService);
        handler.run();
        verify(mockService, times(0)).saveMessage(any(Message.class));
    }



}

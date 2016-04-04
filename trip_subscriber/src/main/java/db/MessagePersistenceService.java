package db;


import parser.Message;

public interface MessagePersistenceService {

    void saveMessage(Message message);

}

package subscriber;

import com.datastax.driver.core.Session;
import db.MessagePersistenceService;
import db.MessagePersistenceServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPubSub;

import javax.sql.DataSource;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Pub/sub subscriber for Jedis queue.
 */
public class MessageSubscriber extends JedisPubSub {

    private static final Logger logger = LoggerFactory.getLogger(MessageSubscriber.class);

    private final MessagePersistenceService service;
    private final ExecutorService pool;

    public MessageSubscriber(DataSource dataSource, Session session) {
        pool = Executors.newFixedThreadPool(4);
        service = new MessagePersistenceServiceImpl(dataSource, session);

        addShutdownHook(session);
    }

    @Override
    public void onMessage(String channel, String message) {
        logger.debug(String.format("Received a message from channel: %s with value: %s", channel, message));
        if (!pool.isShutdown()) {
            pool.execute(new MessageHandler(message, service));
        }
    }

    private void addShutdownHook(final Session session) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.info("The server was shutdown, closing connection pool.");
                pool.shutdown();
                try {
                    pool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
                    session.close();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

}

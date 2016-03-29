package harness;

import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Basic test harness which starts 5 threads, each of which publishes 100 messages
 * per second to Redis pub/sub channel. This is to simulate the 500 concurrent connections
 * the system needs to deal with.
 */
public class TestHarness {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TestHarness.class);


    public static void main(String[] args) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(10);
        JedisPool pool = new JedisPool(config, "localhost");

        try {
            for (int i = 0; i < 5; i++) {
                new Thread(new MessageProducer(pool.getResource(), "trip_updates")).start();
                logger.debug("Created thread number: " + (i + 1));
            }
        } finally {
            pool.close();
        }

    }

}

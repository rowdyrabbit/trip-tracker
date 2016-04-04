import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import subscriber.MessageSubscriber;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static final String DB_DRIVER_CLASS =    "DB_DRIVER_CLASS";
    private static final String DB_URL =             "DB_URL";
    private static final String DB_USERNAME =        "DB_USERNAME";
    private static final String DB_PASSWORD =        "DB_PASSWORD";
    private static final String DB_CP_SIZE =         "DB_CP_SIZE";
    private static final String REDIS_HOST =         "REDIS_HOST";
    private static final String REDIS_PORT =         "REDIS_PORT";
    private static final String REDIS_CHANNEL =      "REDIS_CHANNEL";
    private static final String CSSNDRA_CONTACT_PT = "CASSANDRA_CONTACT_POINT";
    private static final String CSSNDRA_KEY_SPACE  = "CASSANDRA_KEY_SPACE";

    private static final String CONFIG_PROPERTIES = "application.properties";


    public static void main(String[] args) throws IOException {
        Properties properties = getAppConfigProperties();
        JedisPool jedisPool = configureJedisPool(properties);
        final Jedis jedis = jedisPool.getResource();

        DataSource ds = configureDataSource(properties);
        Cluster cluster = configureCassandraCluster(properties);
        Session session = cluster.connect(properties.getProperty(CSSNDRA_KEY_SPACE));

        MessageSubscriber subscriber = new MessageSubscriber(ds, session);
        logger.info("Message subscriber started. Awaiting messages.");

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.info("The server was shutdown, closing Redis connection pool.");
                jedisPool.close();
            }
        });
        jedis.subscribe(subscriber, properties.getProperty(REDIS_CHANNEL));

    }

    private static JedisPool configureJedisPool(Properties props) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        return new JedisPool(poolConfig, props.getProperty(REDIS_HOST), Integer.valueOf(props.getProperty(REDIS_PORT)), 0);
    }

    private static Cluster configureCassandraCluster(Properties props) {
        return Cluster.builder().addContactPoint(props.getProperty(CSSNDRA_CONTACT_PT)).build();
    }

    private static Properties getAppConfigProperties() throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Properties props = new Properties();
        try(InputStream resourceStream = loader.getResourceAsStream(CONFIG_PROPERTIES)) {
            props.load(resourceStream);
        }
        return props;
    }

    private static DataSource configureDataSource(Properties props) {
        BasicDataSource cp = new BasicDataSource();
        cp.setUsername(props.getProperty(DB_USERNAME));
        cp.setPassword(props.getProperty(DB_PASSWORD));
        cp.setDriverClassName(props.getProperty(DB_DRIVER_CLASS));
        cp.setUrl(props.getProperty(DB_URL));
        cp.setInitialSize(Integer.valueOf(props.getProperty(DB_CP_SIZE)));
        return cp;
    }
}

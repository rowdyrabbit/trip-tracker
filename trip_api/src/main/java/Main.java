import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import handlers.TripRequestHandler;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.TripDataServiceImpl;
import spark.ModelAndView;
import spark.template.handlebars.HandlebarsTemplateEngine;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

import static spark.Spark.get;
import static spark.SparkBase.port;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static final String DB_DRIVER_CLASS =    "DB_DRIVER_CLASS";
    private static final String DB_URL =             "DB_URL";
    private static final String DB_USERNAME =        "DB_USERNAME";
    private static final String DB_PASSWORD =        "DB_PASSWORD";
    private static final String DB_CP_SIZE =         "DB_CP_SIZE";
    private static final String CSSNDRA_CONTACT_PT = "CASSANDRA_CONTACT_POINT";
    private static final String CSSNDRA_KEY_SPACE  = "CASSANDRA_KEY_SPACE";
    private static final String SPARK_API_PORT     = "SPARK_API_PORT";

    public static void main(String[] args) throws IOException {
        Properties properties = getAppConfigProperties();

        DataSource ds = configureDataSource(properties);
        Cluster cluster = configureCassandraCluster(properties);
        Session session = cluster.connect(properties.getProperty(CSSNDRA_KEY_SPACE));

        TripRequestHandler handler = new TripRequestHandler(new TripDataServiceImpl(ds, session));

        port(Integer.valueOf(properties.getProperty(SPARK_API_PORT, "4567")));

        get("/api/trips/geocount", (req, resp) -> handler.getNumberOfTripsInGeo(req, resp));

        get("/api/trips/geovalue", (req, resp) -> handler.getTripsStartedOrCompletedInGeo(req, resp));

        get("api/trips/timecount", (req, resp) -> handler.getNumberOfTripsAtTime(req, resp));

        get("/", (req, resp) -> new ModelAndView(new HashMap<>(), "index.hbs"), new HandlebarsTemplateEngine());

        logger.info("API service started, waiting for requests");
    }



    private static Properties getAppConfigProperties() throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        String configFile = "application.properties";
        Properties props = new Properties();
        try(InputStream resourceStream = loader.getResourceAsStream(configFile)) {
            props.load(resourceStream);
        }
        return props;
    }

    private static Cluster configureCassandraCluster(Properties props) {
        return Cluster.builder().addContactPoint(props.getProperty(CSSNDRA_CONTACT_PT)).build();
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

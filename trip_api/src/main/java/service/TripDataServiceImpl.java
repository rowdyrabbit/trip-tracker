package service;


import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import model.GeoTripData;
import model.TimeRange;
import org.apache.commons.dbutils.AsyncQueryRunner;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static com.codahale.metrics.MetricRegistry.name;


public class TripDataServiceImpl implements TripDataService {

    private static final Logger logger = LoggerFactory.getLogger(TripDataServiceImpl.class);

    private static final String JMX_CONSOLE_STATISTICS_NAME = "database-requests";

    private static final String COUNT_TRIPS_IN_GEOHASH = "SELECT trip_id FROM geo_trips WHERE geohash = ?";
    private static final String QUERY_START_STOP_GEOHASHES = "SELECT count(trip_id), sum(fare) FROM orgn_dst_geo_trips WHERE geohash_start like ? or geohash_end like ?;";
    private static final String QUERY_TRIPS_BY_TIME = "SELECT count(*) FROM time_trips WHERE start_time >= ? and end_time <= ?";


    private final ExecutorService asyncQueryService = Executors.newCachedThreadPool();;
    private final DataSource dataSource;
    private final Session session;
    private final PreparedStatement cassandraGeoTripCountQuery;

    static final MetricRegistry dbMetrics = new MetricRegistry();
    private final Timer responses = dbMetrics.timer(name(TripDataServiceImpl.class, JMX_CONSOLE_STATISTICS_NAME));


    public TripDataServiceImpl(DataSource dataSource, Session session) {
        final JmxReporter reporter = JmxReporter.forRegistry(dbMetrics).build();
        reporter.start();

        this.dataSource = dataSource;
        this.session = session;
        cassandraGeoTripCountQuery = session.prepare(COUNT_TRIPS_IN_GEOHASH);

        addShutDownHook(reporter);
    }

    /**
     * Given a set of geohashes, returns the number of (unique) trips which have passed through it.
     *
     * @param geoHashes a list of geohashes to search
     * @return the number of trips that have passed through these geohashes
     */
    @Override
    public long getNumberOfTripsInGeoLocation(List<String> geoHashes) {
        final Timer.Context context = responses.time();
        List<ResultSetFuture> futures = new ArrayList<>();
        try {
            for (String geoHash: geoHashes) {
                ResultSetFuture resultSetFuture = session.executeAsync(cassandraGeoTripCountQuery.bind(geoHash));
                futures.add(resultSetFuture);
            }
            return countAllUniqueTrips(geoHashes, futures);
        } finally {
            context.stop();
        }
    }

    /**
     * Given a time range, count the number of trips that were occurring at that time
     *
     * @param timeRange a time range in UNIX epoch time
     * @return the total number of trips that were occurring during this time
     * @throws SQLException
     */
    @Override
    public long getNumberOfTripsInTimeRange(TimeRange timeRange) throws SQLException {
        final Timer.Context context = responses.time();
        QueryRunner run = new QueryRunner(dataSource);
        try {
            return run.query(QUERY_TRIPS_BY_TIME,
                    new ScalarHandler<Number>(),
                    timeRange.getFromTime(),
                    timeRange.getUntilTime()).longValue();
        } catch (SQLException ex) {
            logger.error(String.format("Failed to query number of trips between  %d and %d", timeRange.getFromTime(), timeRange.getUntilTime()), ex);
            throw ex;
        } finally {
            context.stop();
        }
    }


    /**
     * Given a set of geohashes, returns the number of trips which started or stopped in each geohash and the sum total
     * of all their fares. This function runs each query in parallel for each geohash, rather than running them
     * sequentially.
     *
     * @param geoHashes a list of geohashes to search
     * @return A count of all trips and the total of all their fares
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws SQLException
     */
    @Override
    public GeoTripData getStartFinishTripDataForGeoLocation(List<String> geoHashes) throws InterruptedException, ExecutionException, SQLException {
        final Timer.Context context = responses.time();

        long totalTripCount = 0;
        double totalTripValue = 0;
        List<Future<GeoTripData>> geoTripDataFutures = new ArrayList<>();
        QueryRunner run = new QueryRunner(dataSource);
        AsyncQueryRunner asyncRun = new AsyncQueryRunner(asyncQueryService, run);
        try {
            for (String geoHash : geoHashes) {
                Future<GeoTripData> future = asyncRun.query(QUERY_START_STOP_GEOHASHES, geoTripStats, geoHash + '%', geoHash + '%');
                geoTripDataFutures.add(future);
            }
            for (Future<GeoTripData> future: geoTripDataFutures) {
                GeoTripData data = future.get();
                totalTripCount += data.getTripCount();
                totalTripValue += data.getFareTotal();
                logger.debug("Query result, fare = : " + data.getFareTotal() + " trip count = :" + data.getTripCount() );
            }
        } catch (SQLException ex) {
            logger.error(String.format("Could not get start/finish trip data for all geohashes in: %s", geoHashes.toString()), ex);
            throw ex;
        } catch (Exception ex) {
            logger.error("Exception occurred when calling get on future", ex);
            throw ex;
        } finally {
            context.stop();
        }

        return new GeoTripData(totalTripCount, totalTripValue);
    }



    private long countAllUniqueTrips(List<String> geoHashes, List<ResultSetFuture> futures) {
        Set<String> allTrips = new HashSet<>();
        for (ResultSetFuture future : futures) {
            com.datastax.driver.core.ResultSet rows = future.getUninterruptibly();

            List<Row> allRows = rows.all();
            if (geoHashes.size() == 1) {
                return allRows.size();
            } else {
                // De-duplicate the trip_id fields
                for (Row r: allRows) {
                    allTrips.add(r.getString(0));
                }
            }
        }
        return allTrips.size();
    }


    private void addShutDownHook(JmxReporter reporter) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.info("The server was shutdown, closing connection pool.");
                asyncQueryService.shutdown();
                try {
                    asyncQueryService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
                    session.close();
                    reporter.close();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    private final ResultSetHandler<GeoTripData> geoTripStats = new BeanHandler<GeoTripData>(GeoTripData.class) {
        @Override
        public GeoTripData handle(ResultSet rs) throws SQLException {
            if (rs.next()) {
                BigDecimal value = rs.getBigDecimal("sum");
                long count = rs.getLong("count");

                GeoTripData data = new GeoTripData(count, value != null ? value.doubleValue() : 0);
                return data;
            } else {
                throw new SQLException("Empty result set");
            }
        }
    };
}

package db;


import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import org.apache.commons.dbutils.QueryRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import parser.EventType;
import parser.Message;

import javax.sql.DataSource;
import java.sql.SQLException;

public class MessagePersistenceServiceImpl implements MessagePersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(MessagePersistenceServiceImpl.class);

    // SQL statements for inserting time related data into relational data table
    private static final String INSERT_TIME_TRIP = "INSERT INTO time_trips (trip_id, start_time) values (?, ?) ON CONFLICT DO NOTHING";
    private static final String UPDATE_TIME_TRIP = "UPDATE time_trips SET end_time = ? WHERE trip_id = ?";

    // SQL statements for inserting and updating stop/start geohash trip data into relational table
    private static final String INSERT_ORGN_DST_GEO_TRIP = "INSERT INTO orgn_dst_geo_trips (trip_id, geohash_start) values (?, ?) ON CONFLICT DO NOTHING";
    private static final String UPDATE_DEST_GEO_TRIP = "UPDATE orgn_dst_geo_trips SET geohash_end = ?, fare = ? where trip_id = ?";

    // CQL statement for inserting geohash trip data into Cassandra
    private static final String INSERT_GEO_TRIP = "INSERT INTO geo_trips (geohash, trip_id) VALUES(?, ?)";

    private final DataSource dataSource;
    private final Session session;
    private final PreparedStatement cassandraGeoTripPreparedQuery;

    public MessagePersistenceServiceImpl(DataSource dataSource, Session session) {
        this.dataSource = dataSource;
        this.session = session;
        cassandraGeoTripPreparedQuery = session.prepare(INSERT_GEO_TRIP);
    }

    /**
     * Updates all the data stores with the message data. All queries in this class should be idempotent so that
     * we can implement at least once delivery of messages in the future.
     *
     * @param message the message to persist in the various tables and data stores.
     */
    @Override
    public void saveMessage(Message message) {
        QueryRunner run = new QueryRunner(dataSource);

        insertGeoDataForTrip(message);

        insertTripDataByTime(run, message);

        if (message.getEvent() == EventType.BEGIN) {
            insertOriginDestGeoDataForTrip(run, message);
        } else if (message.getEvent() == EventType.END) {
            updateOriginDestGeoDataForTrip(run, message);
        }
    }

    private void updateOriginDestGeoDataForTrip(QueryRunner run, Message message) {
        try {
            run.update(UPDATE_DEST_GEO_TRIP,
                    message.getGeoHash(),
                    message.getFare(),
                    message.getTripId());
        } catch (SQLException ex) {
            logger.error(String.format("Failed to update message into orgn_dst_geo_trips table: %s", message.toString()), ex);
        }
    }

    private void insertOriginDestGeoDataForTrip(QueryRunner run, Message message) {
        try {
            run.update(INSERT_ORGN_DST_GEO_TRIP,
                    message.getTripId(),
                    message.getGeoHash());
        } catch (SQLException ex) {
            logger.error(String.format("Failed to insert message into orgn_dst_geo_trips table: %s", message.toString()), ex);
        }
    }

    private void insertGeoDataForTrip(Message message) {
        String geoHash = message.getGeoHash();
        for (int i = 1; i <= geoHash.length(); i++) {
           insertTripDataForGeoHash(geoHash.substring(0, i), message.getTripId());
        }
    }

    private void insertTripDataForGeoHash(String geoHash, String tripId) {
        try {
            BoundStatement boundStatement = new BoundStatement(cassandraGeoTripPreparedQuery);
            session.execute(boundStatement.bind(geoHash, tripId));
        } catch (NoHostAvailableException ex) {
            logger.error(String.format("Failed to insert geo trip data for tripId: %s and geoHash: %s", tripId, geoHash), ex);
        }
    }

    private void insertTripDataByTime(QueryRunner run, Message message) {
        if (message.getEvent() == EventType.BEGIN) {
            try {
                run.update(INSERT_TIME_TRIP,
                        message.getTripId(),
                        message.getEpoch());
            } catch (SQLException ex) {
                logger.error(String.format("Failed to insert trip into trip_times table: %s", message.toString()), ex);
            }
        } else if (message.getEvent() == EventType.END) {
            try {
                run.update(UPDATE_TIME_TRIP,
                        message.getEpoch(),
                        message.getTripId());
            } catch (SQLException ex) {
                logger.error(String.format("Failed to update trip end time in trip_times table: %s", message.toString()), ex);
            }
        }
    }
}


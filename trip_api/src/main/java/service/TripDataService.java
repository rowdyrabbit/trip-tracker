package service;


import model.GeoTripData;
import model.TimeRange;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public interface TripDataService {

    long getNumberOfTripsInGeoLocation(List<String> geoHashes);

    long getNumberOfTripsInTimeRange(TimeRange timeRange) throws SQLException;

    GeoTripData getStartFinishTripDataForGeoLocation(List<String> geoHashes) throws InterruptedException, ExecutionException, SQLException;

}

package handlers;


import geo.GeoHashHelper;
import model.BoundingGeoRect;
import model.GeoTripData;
import model.TimeRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import parser.InvalidQueryParamsException;
import parser.RequestParser;
import service.TripDataService;
import spark.Request;
import spark.Response;

import java.util.List;

public class TripRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(TripRequestHandler.class);

    private final TripDataService service;

    public TripRequestHandler(TripDataService service) {
        this.service = service;
    }

    public String getNumberOfTripsAtTime(Request req, Response resp) {
        try {
            TimeRange timeRange = RequestParser.getTimeRangeForParams(req);

            long numberOfTrips = service.getNumberOfTripsInTimeRange(timeRange);

            return String.format("Number of trips that occurred between the epochs %d and %d is: %d",
                    timeRange.getFromTime(), timeRange.getUntilTime(), numberOfTrips);
        } catch (InvalidQueryParamsException ex) {
            resp.status(400);
            return ex.toString();
        } catch (Exception ex) {
            logger.error("Exception occurred whilst serving query request", ex);
            resp.status(500);
            return ex.toString();
        }
    }


    public String getNumberOfTripsInGeo(Request req, Response resp) {
        try {
            BoundingGeoRect boundingBox = RequestParser.getGeoRectForParams(req);

            List<String> geoHashesToSearch = GeoHashHelper.calculateGeohashesWithinSearchArea(boundingBox);

            long result = service.getNumberOfTripsInGeoLocation(geoHashesToSearch);

            return String.format("Number of trips that have passed through this geo rect is: %d", result);
        } catch (InvalidQueryParamsException ex) {
            resp.status(400);
            return ex.toString();
        } catch (Exception ex) {
            logger.error("Exception occurred whilst serving query request", ex);
            resp.status(500);
            return ex.toString();
        }
    }

    public String getTripsStartedOrCompletedInGeo(Request req, Response resp) {
        try {
            BoundingGeoRect boundingBox = RequestParser.getGeoRectForParams(req);
            List<String> geoHashesToSearch = GeoHashHelper.calculateGeohashesWithinSearchArea(boundingBox);

            GeoTripData data = service.getStartFinishTripDataForGeoLocation(geoHashesToSearch);

            return String.format("Number of trips started or stopped in geo rect is: %d, " +
                                    "with a total value of $%.2f", data.getTripCount(), data.getFareTotal());
        } catch (InvalidQueryParamsException ex) {
            resp.status(400);
            return ex.toString();
        } catch (Exception ex) {
            logger.error("Exception occurred whilst serving query request", ex);
            resp.status(500);
            return ex.toString();
        }
    }

}

package parser;


import model.BoundingGeoRect;
import model.TimeRange;
import org.vertexium.type.GeoPoint;
import spark.Request;

public class RequestParser {

    // Prevent unnecessary instantiations
    private RequestParser() {
    }

    /**
     * Parses the request parameters for a from and to time and validates them.
     *
     * @param req the HTTP request
     * @return an object containing the validated time range request parameters
     * @throws InvalidQueryParamsException if the supplied request parameters are invalid
     */
    public static TimeRange getTimeRangeForParams(Request req) throws InvalidQueryParamsException {
        String from = req.queryParams("from");
        String to = req.queryParams("to");

        if (from != null) {
            try {
                // if 'to' param not supplied, search up until now.
                if (to == null || to.isEmpty()) {
                    long now = System.currentTimeMillis();
                    return new TimeRange(Long.valueOf(from), now);
                } else {
                    return new TimeRange(Long.valueOf(from), Long.valueOf(to));
                }
            } catch (NumberFormatException ex) {
                throw new InvalidQueryParamsException(
                        String.format("Request parameters not of numeric type: from='%s', to='%s'", from, to), ex);
            }
        } else {
            throw new InvalidQueryParamsException(
                    String.format("Request parameters are missing: from='%s', to='%s'", from, to));
        }
    }

    /**
     * Parses the request parameters for a NW and SE lat/long point.
     *
     * @param req the HTTP request
     * @return an object containing the validated georect area.
     * @throws InvalidQueryParamsException if the supplied request parameters are invalid
     */
    public static BoundingGeoRect getGeoRectForParams(Request req) throws InvalidQueryParamsException{
        String nwStr = req.queryParams("nw");
        String seStr = req.queryParams("se");
        if (nwStr != null && seStr != null) {

            String[] nw = nwStr.split(",");
            String[] se = seStr.split(",");

            if (nw.length < 2 || se.length < 2) {
                throw new InvalidQueryParamsException(
                        String.format("Lat/long request parameters are missing comma separator: nw='%s', se='%s'", nwStr, seStr));
            }

            try {
                GeoPoint nwPoint = new GeoPoint(Double.valueOf(nw[0]), Double.valueOf(nw[1]));
                GeoPoint sePoint = new GeoPoint(Double.valueOf(se[0]), Double.valueOf(se[1]));
                return new BoundingGeoRect(nwPoint, sePoint);
            } catch (NumberFormatException ex) {
                throw new InvalidQueryParamsException(
                        String.format("Request parameters not of numeric type: nw='%s', se='%s'", nw[0] + ',' + nw[1], se[0] + ',' +se[1]), ex);
            }
        } else {
            throw new InvalidQueryParamsException(
                    String.format("Request parameters are missing: nw='%s', se='%s'", nwStr, seStr));
        }
    }
}

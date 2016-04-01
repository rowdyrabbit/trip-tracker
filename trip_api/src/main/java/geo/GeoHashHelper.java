package geo;


import model.BoundingGeoRect;
import org.locationtech.spatial4j.distance.DistanceUtils;
import org.locationtech.spatial4j.io.GeohashUtils;
import org.locationtech.spatial4j.shape.Rectangle;
import org.locationtech.spatial4j.shape.impl.RectangleImpl;
import org.vertexium.type.GeoHash;
import org.vertexium.type.GeoPoint;
import org.vertexium.type.GeoRect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.locationtech.spatial4j.context.SpatialContext.GEO;

public class GeoHashHelper {


    /**
     * Given a georect (an area bounded by a NW and SE point), find all geohashes that fit within the
     * area. This function will only look at one precision below the precision of the geohash that encompasses the
     * georect, thus the alignment of the geohashes with the georect may not be accurate. This accuracy degrades the
     * larger the georect to search.
     *
     * @param searchArea a representation of the georect to find geohashes for.
     * @return a list of geohashes that fit within the search georect area.
     */
    public static List<String> calculateGeohashesWithinSearchArea(BoundingGeoRect searchArea) {
        GeoPoint northWest = searchArea.getNorthWest();
        GeoPoint southEast = searchArea.getSouthEast();
        GeoRect boundingBox = new GeoRect(northWest, southEast);
        GeoPoint center = GeoPoint.calculateCenter(Arrays.asList(northWest, southEast));


        String hashCodeToSearch = GeohashUtils.encodeLatLon(center.getLatitude(), center.getLongitude(),
                getSmallestEncompassingPrecision(northWest.getLatitude(), northWest.getLongitude(),
                        southEast.getLatitude(), southEast.getLongitude()));

        return removeOutOfBoundsGeoHashes(hashCodeToSearch, boundingBox);
    }

    /**
     * The purpose of this method is to refine the outer boundary of the original geohash we have found.
     * It only looks at one level of precision less than the original bounding georect.
     *
     * @param geoHash the bounding geohash to filter non-overlapping geohashes from.
     * @param boundingBox the search georect
     * @return a list of geohashes which are contained within the search geohash
     */
    protected static List<String> removeOutOfBoundsGeoHashes(String geoHash, GeoRect boundingBox) {
        List<String> result = new ArrayList<>();

        // Don't bother if the precision of the geohash length is large (like 7, 8 or 9)
        if (geoHash.length() >= 7) {
            return Arrays.asList(geoHash);
        }

        String[] subGeoHashes = GeohashUtils.getSubGeohashes(geoHash);
        for (String s: subGeoHashes) {
            GeoHash currHash = new GeoHash(s);
            GeoRect cell = currHash.toGeoRect();
            if (cell.intersects(boundingBox)) {
                result.add(s);
            }
        }
        if (result.size() == 32) {
            return Arrays.asList(geoHash);
        } else {
            return result;
        }
    }

    /**
     * This function returns the geohash length of the smallest geohash which wholly
     * encompasses the NW and SE lat/long values. Note that the values are approximations, taken from
     * https://en.wikipedia.org/wiki/Geohash and does not take into account variations away from the equator.
     *
     * @param nwLat
     * @param nwLng
     * @param seLat
     * @param seLng
     * @return
     */
    protected static int getSmallestEncompassingPrecision(double nwLat, double nwLng, double seLat, double seLng) {
        Rectangle rect = new RectangleImpl(nwLng, seLng, seLat, nwLat, GEO);
        double height = DistanceUtils.DEG_TO_KM * rect.getHeight();
        double width = DistanceUtils.DEG_TO_KM * rect.getWidth();

        if (height <= 4.8d/1000 && width <= 4.8d/1000) {
            return 9;
        } else if (height <= 19d/1000 && width <= 38.2d/1000) {
            return 8;
        } else if (height <= 152.4d/1000 && width <= 152.9d/1000) {
            return 7;
        } else if (height <= 609.4d/1000 && width <= 1.2d) {
            return 6;
        } else if (height <= 4.9d && width <= 4.9d) {
            return 5;
        } else if (height <= 19.5d && width <= 39.1d) {
            return 4;
        } else if (height <= 156d && width <= 156.5d) {
            return 3;
        } else if (height <= 624.1d && width <= 1252.3d) {
            return 2;
        } else {
            return 1;
        }
    }


}

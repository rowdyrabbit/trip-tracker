package model;


import org.vertexium.type.GeoPoint;

public class BoundingGeoRect {

    private GeoPoint northWest;
    private GeoPoint southEast;

    public BoundingGeoRect(GeoPoint northWest, GeoPoint southEast) {
        this.northWest = northWest;
        this.southEast = southEast;
    }

    public GeoPoint getNorthWest() {
        return northWest;
    }

    public GeoPoint getSouthEast() {
        return southEast;
    }

}

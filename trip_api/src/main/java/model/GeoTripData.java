package model;


public class GeoTripData {

    private long tripCount;
    private double fareTotal;

    public GeoTripData(long tripCount, double fareTotal) {
        this.tripCount = tripCount;
        this.fareTotal = fareTotal;
    }

    public long getTripCount() {
        return tripCount;
    }

    public double getFareTotal() {
        return fareTotal;
    }

}

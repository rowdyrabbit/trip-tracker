package parser;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.vertexium.type.GeoHash;

import java.sql.Timestamp;
import java.time.Instant;

public class Message {

    private static final int GEOHASH_PRECISION = 9;

    @JsonProperty(value="event", required=true)
    private EventType event;
    @JsonProperty(value="tripId", required=true)
    private String tripId;
    @JsonProperty(value="lat", required=true)
    private double lat;
    @JsonProperty(value="lng", required=true)
    private double lng;
    @JsonProperty("fare")
    private Float fare;
    @JsonProperty(value="epoch", required=true)
    private long timestamp;

    public Message() {

    }

    public Message(EventType event, String tripId, double lat, double lng, long timestamp) {
        this.event = event;
        this.tripId = tripId;
        this.lat = lat;
        this.lng = lng;
        this.timestamp = timestamp;
    }

    public Message(EventType event, String tripId, double lat, double lng, Float fare, long timestamp) {
        this(event, tripId, lat, lng, timestamp);
        this.fare = fare;
    }

    public String getGeoHash() {
        GeoHash hash = new GeoHash(lat, lng, GEOHASH_PRECISION);
        return hash.getHash();
    }

    public EventType getEvent() {
        return event;
    }

    public String getTripId() {
        return tripId;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public Float getFare() {
        return fare;
    }

    public Long getEpoch() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Message{" +
                "event=" + event +
                ", tripId='" + tripId + '\'' +
                ", lat=" + lat +
                ", lng=" + lng +
                ", fare=" + fare +
                ", timestamp=" + timestamp +
                '}';
    }
}

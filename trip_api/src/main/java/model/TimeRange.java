package model;


public class TimeRange {

    private long fromTime;
    private long untilTime;

    public TimeRange(long fromTime, long untilTime) {
        this.fromTime = fromTime;
        this.untilTime = untilTime;
    }

    public long getFromTime() {
        return fromTime;
    }

    public long getUntilTime() {
        return untilTime;
    }

}

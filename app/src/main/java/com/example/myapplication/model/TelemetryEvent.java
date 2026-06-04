package com.example.myapplication.model;

public class TelemetryEvent {
    public static final String EXPOSURE = "exposure";
    public static final String CLICK = "click";
    public static final String LIKE = "like";
    public static final String COLLECT = "collect";
    public static final String SHARE = "share";

    private final String adId;
    private final String eventType;
    private final String channel;
    private final int position;
    private final String extra;
    private final long timestamp;

    public TelemetryEvent(String adId, String eventType, String channel, int position, String extra) {
        this.adId = adId;
        this.eventType = eventType;
        this.channel = channel;
        this.position = position;
        this.extra = extra;
        this.timestamp = System.currentTimeMillis();
    }

    public String getAdId() {
        return adId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getChannel() {
        return channel;
    }

    public int getPosition() {
        return position;
    }

    public String getExtra() {
        return extra;
    }

    public long getTimestamp() {
        return timestamp;
    }
}

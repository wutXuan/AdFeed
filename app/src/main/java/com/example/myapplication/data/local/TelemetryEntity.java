package com.example.myapplication.data.local;

import com.example.myapplication.model.TelemetryEvent;

public class TelemetryEntity {
    public long id;
    public String adId;
    public String eventType;
    public String channel;
    public int position;
    public String extra;
    public long timestamp;

    public static TelemetryEntity fromEvent(TelemetryEvent event) {
        TelemetryEntity entity = new TelemetryEntity();
        entity.adId = event.getAdId();
        entity.eventType = event.getEventType();
        entity.channel = event.getChannel();
        entity.position = event.getPosition();
        entity.extra = event.getExtra();
        entity.timestamp = event.getTimestamp();
        return entity;
    }
}

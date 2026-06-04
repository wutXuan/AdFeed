package com.example.myapplication.data.local;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class AdDao {
    private final AppDatabase database;

    AdDao(AppDatabase database) {
        this.database = database;
    }

    public int countAds() {
        try (Cursor cursor = database.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM ads", null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    public void upsertAds(List<AdEntity> ads) {
        SQLiteDatabase db = database.getWritableDatabase();
        db.beginTransaction();
        try {
            for (AdEntity ad : ads) {
                db.insertWithOnConflict("ads", null, toValues(ad), SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public List<AdEntity> getAds(String channel, int limit, int offset) {
        return query("channel = ?", new String[]{channel}, limit, offset);
    }

    public List<AdEntity> getAdsByTag(String channel, String tag, int limit, int offset) {
        return query("channel = ? AND tags LIKE ?", new String[]{channel, "%" + tag + "%"}, limit, offset);
    }

    public List<AdEntity> getAllAds() {
        return query(null, null, 500, 0);
    }

    public AdEntity getAd(String id) {
        List<AdEntity> rows = query("id = ?", new String[]{id}, 1, 0);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void updateLike(String id, boolean liked, int likeCount) {
        ContentValues values = new ContentValues();
        values.put("liked", liked ? 1 : 0);
        values.put("likeCount", likeCount);
        database.getWritableDatabase().update("ads", values, "id = ?", new String[]{id});
    }

    public void updateCollected(String id, boolean collected) {
        ContentValues values = new ContentValues();
        values.put("collected", collected ? 1 : 0);
        database.getWritableDatabase().update("ads", values, "id = ?", new String[]{id});
    }

    public void incrementClick(String id) {
        database.getWritableDatabase().execSQL("UPDATE ads SET clickCount = clickCount + 1 WHERE id = ?", new Object[]{id});
    }

    public void incrementExposure(String id) {
        database.getWritableDatabase().execSQL("UPDATE ads SET exposureCount = exposureCount + 1 WHERE id = ?", new Object[]{id});
    }

    public void updateAiMeta(String id, String summary, String tags, String aiReason, String playfulCopy) {
        ContentValues values = new ContentValues();
        values.put("summary", summary);
        values.put("tags", tags);
        values.put("aiReason", aiReason);
        values.put("playfulCopy", playfulCopy);
        database.getWritableDatabase().update("ads", values, "id = ?", new String[]{id});
    }

    public void insertTelemetry(TelemetryEntity event) {
        ContentValues values = new ContentValues();
        values.put("adId", event.adId);
        values.put("eventType", event.eventType);
        values.put("channel", event.channel);
        values.put("position", event.position);
        values.put("extra", event.extra);
        values.put("timestamp", event.timestamp);
        database.getWritableDatabase().insert("telemetry", null, values);
    }

    private List<AdEntity> query(String selection, String[] args, int limit, int offset) {
        List<AdEntity> rows = new ArrayList<>();
        String limitClause = limit + " OFFSET " + offset;
        try (Cursor cursor = database.getReadableDatabase().query(
                "ads",
                null,
                selection,
                args,
                null,
                null,
                "channel ASC, sortOrder ASC",
                limitClause)) {
            while (cursor.moveToNext()) {
                rows.add(fromCursor(cursor));
            }
        }
        return rows;
    }

    private ContentValues toValues(AdEntity ad) {
        ContentValues values = new ContentValues();
        values.put("id", ad.id);
        values.put("channel", ad.channel);
        values.put("type", ad.type);
        values.put("title", ad.title);
        values.put("brand", ad.brand);
        values.put("description", ad.description);
        values.put("mediaUrl", ad.mediaUrl);
        values.put("thumbnailUrl", ad.thumbnailUrl);
        values.put("targetUrl", ad.targetUrl);
        values.put("liked", ad.liked ? 1 : 0);
        values.put("collected", ad.collected ? 1 : 0);
        values.put("likeCount", ad.likeCount);
        values.put("exposureCount", ad.exposureCount);
        values.put("clickCount", ad.clickCount);
        values.put("summary", ad.summary);
        values.put("tags", ad.tags);
        values.put("aiReason", ad.aiReason);
        values.put("playfulCopy", ad.playfulCopy);
        values.put("sortOrder", ad.sortOrder);
        return values;
    }

    private AdEntity fromCursor(Cursor cursor) {
        AdEntity ad = new AdEntity();
        ad.id = getString(cursor, "id");
        ad.channel = getString(cursor, "channel");
        ad.type = getString(cursor, "type");
        ad.title = getString(cursor, "title");
        ad.brand = getString(cursor, "brand");
        ad.description = getString(cursor, "description");
        ad.mediaUrl = getString(cursor, "mediaUrl");
        ad.thumbnailUrl = getString(cursor, "thumbnailUrl");
        ad.targetUrl = getString(cursor, "targetUrl");
        ad.liked = getInt(cursor, "liked") == 1;
        ad.collected = getInt(cursor, "collected") == 1;
        ad.likeCount = getInt(cursor, "likeCount");
        ad.exposureCount = getInt(cursor, "exposureCount");
        ad.clickCount = getInt(cursor, "clickCount");
        ad.summary = getString(cursor, "summary");
        ad.tags = getString(cursor, "tags");
        ad.aiReason = getString(cursor, "aiReason");
        ad.playfulCopy = getString(cursor, "playfulCopy");
        ad.sortOrder = getInt(cursor, "sortOrder");
        return ad;
    }

    private String getString(Cursor cursor, String column) {
        int index = cursor.getColumnIndexOrThrow(column);
        return cursor.isNull(index) ? null : cursor.getString(index);
    }

    private int getInt(Cursor cursor, String column) {
        int index = cursor.getColumnIndexOrThrow(column);
        return cursor.isNull(index) ? 0 : cursor.getInt(index);
    }
}

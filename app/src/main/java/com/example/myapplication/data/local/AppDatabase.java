package com.example.myapplication.data.local;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AppDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "ai_ad_feed.db";
    private static final int DATABASE_VERSION = 2;
    private static volatile AppDatabase instance;
    private final AdDao adDao = new AdDao(this);

    private AppDatabase(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = new AppDatabase(context);
                }
            }
        }
        return instance;
    }

    public AdDao adDao() {
        return adDao;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS ads ("
                + "id TEXT PRIMARY KEY NOT NULL,"
                + "channel TEXT,"
                + "type TEXT,"
                + "title TEXT,"
                + "brand TEXT,"
                + "description TEXT,"
                + "mediaUrl TEXT,"
                + "thumbnailUrl TEXT,"
                + "targetUrl TEXT,"
                + "liked INTEGER NOT NULL DEFAULT 0,"
                + "collected INTEGER NOT NULL DEFAULT 0,"
                + "likeCount INTEGER NOT NULL DEFAULT 0,"
                + "exposureCount INTEGER NOT NULL DEFAULT 0,"
                + "clickCount INTEGER NOT NULL DEFAULT 0,"
                + "summary TEXT,"
                + "tags TEXT,"
                + "aiReason TEXT,"
                + "playfulCopy TEXT,"
                + "sortOrder INTEGER NOT NULL DEFAULT 0"
                + ")");
        db.execSQL("CREATE TABLE IF NOT EXISTS telemetry ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "adId TEXT,"
                + "eventType TEXT,"
                + "channel TEXT,"
                + "position INTEGER,"
                + "extra TEXT,"
                + "timestamp INTEGER"
                + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS telemetry");
        db.execSQL("DROP TABLE IF EXISTS ads");
        onCreate(db);
    }
}

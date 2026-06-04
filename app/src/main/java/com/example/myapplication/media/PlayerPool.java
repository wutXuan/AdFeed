package com.example.myapplication.media;

import android.content.Context;

import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class PlayerPool {
    private static volatile PlayerPool instance;

    private final Context appContext;
    private final int maxPlayers;
    private final Queue<ExoPlayer> idlePlayers = new ArrayDeque<>();
    private final Queue<String> activeOrder = new ArrayDeque<>();
    private final Map<String, ExoPlayer> activePlayers = new HashMap<>();
    private boolean muted = true;

    private PlayerPool(Context context, int maxPlayers) {
        this.appContext = context.getApplicationContext();
        this.maxPlayers = maxPlayers;
    }

    public static PlayerPool getInstance(Context context) {
        if (instance == null) {
            synchronized (PlayerPool.class) {
                if (instance == null) {
                    instance = new PlayerPool(context, 2);
                }
            }
        }
        return instance;
    }

    public synchronized ExoPlayer acquire(String key, String mediaUrl) {
        ExoPlayer existing = activePlayers.get(key);
        if (existing != null) {
            return existing;
        }
        while (activePlayers.size() >= maxPlayers && !activeOrder.isEmpty()) {
            release(activeOrder.poll());
        }
        ExoPlayer player = idlePlayers.poll();
        if (player == null) {
            player = new ExoPlayer.Builder(appContext).build();
        }
        player.setMediaItem(MediaItem.fromUri(mediaUrl));
        player.setVolume(muted ? 0f : 1f);
        player.prepare();
        activePlayers.put(key, player);
        activeOrder.offer(key);
        return player;
    }

    public synchronized void release(String key) {
        if (key == null) {
            return;
        }
        ExoPlayer player = activePlayers.remove(key);
        activeOrder.remove(key);
        if (player == null) {
            return;
        }
        player.pause();
        player.clearMediaItems();
        if (idlePlayers.size() < maxPlayers) {
            idlePlayers.offer(player);
        } else {
            player.release();
        }
    }

    public synchronized void setMuted(boolean muted) {
        this.muted = muted;
        for (ExoPlayer player : activePlayers.values()) {
            player.setVolume(muted ? 0f : 1f);
        }
    }

    public synchronized boolean toggleMuted() {
        setMuted(!muted);
        return muted;
    }

    public synchronized boolean isMuted() {
        return muted;
    }

    public synchronized void releaseAll() {
        for (String key : new ArrayDeque<>(activeOrder)) {
            release(key);
        }
        while (!idlePlayers.isEmpty()) {
            idlePlayers.poll().release();
        }
    }
}

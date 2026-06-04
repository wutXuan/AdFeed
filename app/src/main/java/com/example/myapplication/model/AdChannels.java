package com.example.myapplication.model;

public final class AdChannels {
    public static final String FEATURED = "精选";
    public static final String ECOMMERCE = "电商";
    public static final String LOCAL = "本地";

    private AdChannels() {
    }

    public static String[] all() {
        return new String[]{FEATURED, ECOMMERCE, LOCAL};
    }
}

package com.example.myapplication.ai;

import com.example.myapplication.model.AdItem;

import java.util.ArrayList;
import java.util.List;

public class SearchResult {
    private final AdItem adItem;
    private final String reason;
    private final List<String> matchedTags;

    public SearchResult(AdItem adItem, String reason, List<String> matchedTags) {
        this.adItem = adItem;
        this.reason = reason;
        this.matchedTags = matchedTags == null ? new ArrayList<>() : new ArrayList<>(matchedTags);
    }

    public AdItem getAdItem() {
        return adItem;
    }

    public String getReason() {
        return reason;
    }

    public List<String> getMatchedTags() {
        return matchedTags;
    }
}

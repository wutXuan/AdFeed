package com.example.myapplication.ai;

import java.util.ArrayList;
import java.util.List;

public class AiMeta {
    private final String summary;
    private final List<String> tags;
    private final String reason;
    private final String playfulCopy;

    public AiMeta(String summary, List<String> tags, String reason, String playfulCopy) {
        this.summary = summary;
        this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
        this.reason = reason;
        this.playfulCopy = playfulCopy;
    }

    public String getSummary() {
        return summary;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getReason() {
        return reason;
    }

    public String getPlayfulCopy() {
        return playfulCopy;
    }
}

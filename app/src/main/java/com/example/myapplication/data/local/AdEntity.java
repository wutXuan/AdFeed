package com.example.myapplication.data.local;

import com.example.myapplication.model.AdItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdEntity {
    public String id = "";
    public String channel;
    public String type;
    public String title;
    public String brand;
    public String description;
    public String mediaUrl;
    public String thumbnailUrl;
    public String targetUrl;
    public boolean liked;
    public boolean collected;
    public int likeCount;
    public int exposureCount;
    public int clickCount;
    public String summary;
    public String tags;
    public String aiReason;
    public String playfulCopy;
    public int sortOrder;

    public static AdEntity fromItem(AdItem item) {
        AdEntity entity = new AdEntity();
        entity.id = item.getId();
        entity.channel = item.getChannel();
        entity.type = item.getType();
        entity.title = item.getTitle();
        entity.brand = item.getBrand();
        entity.description = item.getDescription();
        entity.mediaUrl = item.getMediaUrl();
        entity.thumbnailUrl = item.getThumbnailUrl();
        entity.targetUrl = item.getTargetUrl();
        entity.liked = item.isLiked();
        entity.collected = item.isCollected();
        entity.likeCount = item.getLikeCount();
        entity.exposureCount = item.getExposureCount();
        entity.clickCount = item.getClickCount();
        entity.summary = item.getSummary();
        entity.tags = joinTags(item.getTags());
        entity.aiReason = item.getAiReason();
        entity.playfulCopy = item.getPlayfulCopy();
        entity.sortOrder = item.getSortOrder();
        return entity;
    }

    public AdItem toItem() {
        AdItem item = new AdItem();
        item.setId(id);
        item.setChannel(channel);
        item.setType(type);
        item.setTitle(title);
        item.setBrand(brand);
        item.setDescription(description);
        item.setMediaUrl(mediaUrl);
        item.setThumbnailUrl(thumbnailUrl);
        item.setTargetUrl(targetUrl);
        item.setLiked(liked);
        item.setCollected(collected);
        item.setLikeCount(likeCount);
        item.setExposureCount(exposureCount);
        item.setClickCount(clickCount);
        item.setSummary(summary);
        item.setTags(splitTags(tags));
        item.setAiReason(aiReason);
        item.setPlayfulCopy(playfulCopy);
        item.setSortOrder(sortOrder);
        return item;
    }

    public static String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String tag : tags) {
            if (tag == null || tag.trim().isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(",");
            }
            builder.append(tag.trim());
        }
        return builder.toString();
    }

    public static List<String> splitTags(String tags) {
        if (tags == null || tags.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(tags.split("\\s*,\\s*")));
    }
}

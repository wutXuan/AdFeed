package com.example.myapplication.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AdItem {
    public static final String TYPE_LARGE_IMAGE = "large_image";
    public static final String TYPE_SMALL_IMAGE = "small_image";
    public static final String TYPE_VIDEO = "video";

    private String id;
    private String channel;
    private String type;
    private String title;
    private String brand;
    private String description;
    private String mediaUrl;
    private String thumbnailUrl;
    private String targetUrl;
    private boolean liked;
    private boolean collected;
    private int likeCount;
    private int exposureCount;
    private int clickCount;
    private String summary;
    private List<String> tags = new ArrayList<>();
    private String aiReason;
    private String playfulCopy;
    private int sortOrder;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public boolean isLiked() {
        return liked;
    }

    public void setLiked(boolean liked) {
        this.liked = liked;
    }

    public boolean isCollected() {
        return collected;
    }

    public void setCollected(boolean collected) {
        this.collected = collected;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getExposureCount() {
        return exposureCount;
    }

    public void setExposureCount(int exposureCount) {
        this.exposureCount = exposureCount;
    }

    public int getClickCount() {
        return clickCount;
    }

    public void setClickCount(int clickCount) {
        this.clickCount = clickCount;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null ? new ArrayList<>() : new ArrayList<>(tags);
    }

    public String getAiReason() {
        return aiReason;
    }

    public void setAiReason(String aiReason) {
        this.aiReason = aiReason;
    }

    public String getPlayfulCopy() {
        return playfulCopy;
    }

    public void setPlayfulCopy(String playfulCopy) {
        this.playfulCopy = playfulCopy;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean needsAiMeta() {
        return isBlank(summary) || isBlank(aiReason) || tags == null || tags.isEmpty();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AdItem)) {
            return false;
        }
        AdItem adItem = (AdItem) o;
        return liked == adItem.liked
                && collected == adItem.collected
                && likeCount == adItem.likeCount
                && exposureCount == adItem.exposureCount
                && clickCount == adItem.clickCount
                && sortOrder == adItem.sortOrder
                && Objects.equals(id, adItem.id)
                && Objects.equals(channel, adItem.channel)
                && Objects.equals(type, adItem.type)
                && Objects.equals(title, adItem.title)
                && Objects.equals(brand, adItem.brand)
                && Objects.equals(description, adItem.description)
                && Objects.equals(mediaUrl, adItem.mediaUrl)
                && Objects.equals(thumbnailUrl, adItem.thumbnailUrl)
                && Objects.equals(targetUrl, adItem.targetUrl)
                && Objects.equals(summary, adItem.summary)
                && Objects.equals(tags, adItem.tags)
                && Objects.equals(aiReason, adItem.aiReason)
                && Objects.equals(playfulCopy, adItem.playfulCopy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, channel, type, title, brand, description, mediaUrl, thumbnailUrl,
                targetUrl, liked, collected, likeCount, exposureCount, clickCount, summary, tags,
                aiReason, playfulCopy, sortOrder);
    }
}

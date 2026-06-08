package com.example.myapplication.data;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapplication.ai.AiMeta;
import com.example.myapplication.ai.AiRepository;
import com.example.myapplication.ai.SearchResult;
import com.example.myapplication.data.local.AdDao;
import com.example.myapplication.data.local.AdEntity;
import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.TelemetryEntity;
import com.example.myapplication.data.mock.MockAds;
import com.example.myapplication.model.AdChannels;
import com.example.myapplication.model.AdItem;
import com.example.myapplication.model.TelemetryEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdRepository {
    public interface DataCallback<T> {
        void onData(T data);
    }

    private static final int PAGE_SIZE = 6;
    private static volatile AdRepository instance;

    private final AdDao adDao;
    private final AiRepository aiRepository;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final MutableLiveData<List<AdItem>> adsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<String>> tagsLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> refreshingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> loadingMoreLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> channelLiveData = new MutableLiveData<>(AdChannels.FEATURED);
    private final MutableLiveData<String> selectedTagLiveData = new MutableLiveData<>(null);
    private final Map<String, MutableLiveData<AdItem>> detailLiveData = new HashMap<>();
    private final Map<String, Integer> offsets = new HashMap<>();
    private final Set<String> aiInFlight = new HashSet<>();
    private final AtomicBoolean loadingMore = new AtomicBoolean(false);

    private volatile String currentChannel = AdChannels.FEATURED;
    private volatile String currentTag;

    private AdRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        adDao = database.adDao();
        aiRepository = AiRepository.getInstance();
        executor.execute(this::seedIfNeeded);
    }

    public static AdRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (AdRepository.class) {
                if (instance == null) {
                    instance = new AdRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public LiveData<List<AdItem>> observeAds() {
        return adsLiveData;
    }

    public LiveData<List<String>> observeTags() {
        return tagsLiveData;
    }

    public LiveData<Boolean> observeRefreshing() {
        return refreshingLiveData;
    }

    public LiveData<Boolean> observeLoadingMore() {
        return loadingMoreLiveData;
    }

    public LiveData<String> observeChannel() {
        return channelLiveData;
    }

    public LiveData<String> observeSelectedTag() {
        return selectedTagLiveData;
    }

    public LiveData<AdItem> observeAd(String id) {
        MutableLiveData<AdItem> liveData = detailLiveData.get(id);
        if (liveData == null) {
            liveData = new MutableLiveData<>();
            detailLiveData.put(id, liveData);
        }
        reloadAd(id);
        return liveData;
    }

    public void refresh(String channel, String tag) {
        currentChannel = channel;
        currentTag = tag;
        channelLiveData.setValue(channel);
        selectedTagLiveData.setValue(tag);
        refreshingLiveData.setValue(true);
        String key = key(channel, tag);
        offsets.put(key, 0);
        executor.execute(() -> {
            seedIfNeeded();
            List<AdItem> rows = query(channel, tag, 0, PAGE_SIZE);
            offsets.put(key, rows.size());
            if (key.equals(currentKey())) {
                adsLiveData.postValue(rows);
                tagsLiveData.postValue(queryTags(channel));
            }
            refreshingLiveData.postValue(false);
            ensureAiForAds(rows);
        });
    }

    public void loadMore() {
        if (!loadingMore.compareAndSet(false, true)) {
            return;
        }
        loadingMoreLiveData.setValue(true);
        String channel = currentChannel;
        String tag = currentTag;
        String key = key(channel, tag);
        int offset = offsets.containsKey(key) ? offsets.get(key) : 0;
        executor.execute(() -> {
            List<AdItem> next = query(channel, tag, offset, PAGE_SIZE);
            offsets.put(key, offset + next.size());
            if (!next.isEmpty() && key.equals(currentKey())) {
                List<AdItem> merged = new ArrayList<>();
                List<AdItem> current = adsLiveData.getValue();
                if (current != null) {
                    merged.addAll(current);
                }
                merged.addAll(next);
                adsLiveData.postValue(merged);
                ensureAiForAds(next);
            }
            loadingMore.set(false);
            loadingMoreLiveData.postValue(false);
        });
    }

    public void clearTag() {
        refresh(currentChannel, null);
    }

    public void toggleLike(AdItem item) {
        executor.execute(() -> {
            AdEntity entity = adDao.getAd(item.getId());
            if (entity == null) {
                return;
            }
            boolean liked = !entity.liked;
            int likeCount = Math.max(0, entity.likeCount + (liked ? 1 : -1));
            adDao.updateLike(entity.id, liked, likeCount);
            insertTelemetry(new TelemetryEvent(entity.id, TelemetryEvent.LIKE, entity.channel, entity.sortOrder, String.valueOf(liked)));
            reloadAfterMutation(entity.id);
        });
    }

    public void toggleCollected(AdItem item) {
        executor.execute(() -> {
            AdEntity entity = adDao.getAd(item.getId());
            if (entity == null) {
                return;
            }
            boolean collected = !entity.collected;
            adDao.updateCollected(entity.id, collected);
            insertTelemetry(new TelemetryEvent(entity.id, TelemetryEvent.COLLECT, entity.channel, entity.sortOrder, String.valueOf(collected)));
            reloadAfterMutation(entity.id);
        });
    }

    public void trackClick(AdItem item) {
        executor.execute(() -> {
            adDao.incrementClick(item.getId());
            insertTelemetry(new TelemetryEvent(item.getId(), TelemetryEvent.CLICK, item.getChannel(), item.getSortOrder(), ""));
            reloadAfterMutation(item.getId());
        });
    }

    public void trackExposure(AdItem item) {
        executor.execute(() -> {
            adDao.incrementExposure(item.getId());
            insertTelemetry(new TelemetryEvent(item.getId(), TelemetryEvent.EXPOSURE, item.getChannel(), item.getSortOrder(), ""));
            reloadAfterMutation(item.getId());
        });
    }

    public void trackShare(AdItem item) {
        executor.execute(() -> insertTelemetry(new TelemetryEvent(item.getId(), TelemetryEvent.SHARE, item.getChannel(), item.getSortOrder(), "")));
    }

    public void ensureAiForAds(List<AdItem> ads) {
        if (ads == null || ads.isEmpty()) {
            return;
        }
        for (AdItem item : ads) {
            if (!item.needsAiMeta()) {
                continue;
            }
            synchronized (aiInFlight) {
                if (aiInFlight.contains(item.getId())) {
                    continue;
                }
                aiInFlight.add(item.getId());
            }
            aiRepository.generateAdMeta(item, new AiRepository.ResultCallback<AiMeta>() {
                @Override
                public void onSuccess(AiMeta result) {
                    executor.execute(() -> {
                        adDao.updateAiMeta(item.getId(),
                                result.getSummary(),
                                AdEntity.joinTags(result.getTags()),
                                result.getReason(),
                                result.getPlayfulCopy());
                        synchronized (aiInFlight) {
                            aiInFlight.remove(item.getId());
                        }
                        reloadAfterMutation(item.getId());
                    });
                }

                @Override
                public void onError(Throwable throwable) {
                    synchronized (aiInFlight) {
                        aiInFlight.remove(item.getId());
                    }
                }
            });
        }
    }

    public void searchAllAds(String message, DataCallback<List<SearchResult>> callback) {
        executor.execute(() -> {
            seedIfNeeded();
            List<AdItem> candidates = map(adDao.getAllAds());
            mainHandler.post(() -> aiRepository.searchAds(message, candidates, new AiRepository.ResultCallback<List<SearchResult>>() {
                @Override
                public void onSuccess(List<SearchResult> result) {
                    callback.onData(result);
                }

                @Override
                public void onError(Throwable throwable) {
                    callback.onData(new ArrayList<>());
                }
            }));
        });
    }

    private void reloadAfterMutation(String adId) {
        reloadCurrentPage();
        reloadAd(adId);
    }

    private void reloadCurrentPage() {
        String channel = currentChannel;
        String tag = currentTag;
        String key = key(channel, tag);
        int size = Math.max(PAGE_SIZE, offsets.containsKey(key) ? offsets.get(key) : PAGE_SIZE);
        List<AdItem> rows = query(channel, tag, 0, size);
        if (key.equals(currentKey())) {
            adsLiveData.postValue(rows);
            tagsLiveData.postValue(queryTags(channel));
        }
    }

    private void reloadAd(String id) {
        executor.execute(() -> {
            AdEntity entity = adDao.getAd(id);
            MutableLiveData<AdItem> liveData = detailLiveData.get(id);
            if (entity != null && liveData != null) {
                liveData.postValue(entity.toItem());
            }
        });
    }

    private void seedIfNeeded() {
        List<AdEntity> entities = new ArrayList<>();
        for (AdItem item : MockAds.create()) {
            entities.add(AdEntity.fromItem(item));
        }
        if (adDao.countAds() > 0) {
            refreshSeedMedia(entities);
            return;
        }
        adDao.upsertAds(entities);
    }

    private void refreshSeedMedia(List<AdEntity> seedAds) {
        for (AdEntity seedAd : seedAds) {
            AdEntity existing = adDao.getAd(seedAd.id);
            if (existing == null) {
                List<AdEntity> single = new ArrayList<>();
                single.add(seedAd);
                adDao.upsertAds(single);
                continue;
            }
            if (!TextUtils.equals(existing.mediaUrl, seedAd.mediaUrl)
                    || !TextUtils.equals(existing.thumbnailUrl, seedAd.thumbnailUrl)) {
                adDao.updateMedia(seedAd.id, seedAd.mediaUrl, seedAd.thumbnailUrl);
            }
        }
    }

    private List<AdItem> query(String channel, String tag, int offset, int limit) {
        if (TextUtils.isEmpty(tag)) {
            return map(adDao.getAds(channel, limit, offset));
        }
        return map(adDao.getAdsByTag(channel, tag, limit, offset));
    }

    private List<String> queryTags(String channel) {
        List<AdItem> rows = map(adDao.getAds(channel, 100, 0));
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        for (AdItem item : rows) {
            tags.addAll(item.getTags());
        }
        return new ArrayList<>(tags);
    }

    private List<AdItem> map(List<AdEntity> entities) {
        List<AdItem> items = new ArrayList<>();
        if (entities == null) {
            return items;
        }
        for (AdEntity entity : entities) {
            items.add(entity.toItem());
        }
        return items;
    }

    private void insertTelemetry(TelemetryEvent event) {
        adDao.insertTelemetry(TelemetryEntity.fromEvent(event));
    }

    private String currentKey() {
        return key(currentChannel, currentTag);
    }

    private String key(String channel, String tag) {
        return channel + "::" + (tag == null ? "" : tag);
    }
}

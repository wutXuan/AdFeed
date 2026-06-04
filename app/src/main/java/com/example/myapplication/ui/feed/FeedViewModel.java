package com.example.myapplication.ui.feed;

import android.app.Application;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.myapplication.data.AdRepository;
import com.example.myapplication.model.AdChannels;
import com.example.myapplication.model.AdItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 信息流页面的 ViewModel。
 *
 * ViewModel 的作用是帮 Fragment 保存页面状态，并把 UI 操作转发给数据层。
 * 这样屏幕旋转、页面重建时，频道、标签、滚动位置等状态不容易丢。
 */
public class FeedViewModel extends AndroidViewModel {

    // Repository 是数据入口，页面不直接操作数据库或网络。
    private final AdRepository repository;

    // 保存每个频道/标签组合对应的 RecyclerView 滚动状态。
    private final Map<String, Parcelable> scrollStates = new HashMap<>();

    // 当前选中的频道，默认是精选。
    private String currentChannel = AdChannels.FEATURED;

    // 当前选中的标签；null 表示没有筛选标签。
    private String selectedTag;

    public FeedViewModel(@NonNull Application application) {
        super(application);

        // 通过 Application 拿到全局 Repository 单例。
        repository = AdRepository.getInstance(application);

        // ViewModel 创建时，先加载默认频道的数据。
        repository.refresh(currentChannel, selectedTag);
    }

    public LiveData<List<AdItem>> getAds() {
        // Fragment 观察这个 LiveData 后，广告列表变化会自动刷新 UI。
        return repository.observeAds();
    }

    public LiveData<List<String>> getTags() {
        // 当前频道可用的标签列表。
        return repository.observeTags();
    }

    public LiveData<Boolean> getRefreshing() {
        // 下拉刷新是否正在进行。
        return repository.observeRefreshing();
    }

    public LiveData<Boolean> getLoadingMore() {
        // 上拉加载更多是否正在进行。
        return repository.observeLoadingMore();
    }

    public String getCurrentChannel() {
        return currentChannel;
    }

    public String getSelectedTag() {
        return selectedTag;
    }

    public void selectChannel(String channel) {
        // 如果传入频道为空，或者用户点的是当前频道，就不重复刷新。
        if (channel == null || channel.equals(currentChannel)) {
            return;
        }

        // 切换频道。
        currentChannel = channel;

        // 切换频道时清空标签筛选，避免“精选的标签”带到“电商频道”里。
        selectedTag = null;

        // 让 Repository 重新加载新频道的数据。
        repository.refresh(currentChannel, selectedTag);
    }

    public void refresh() {
        // 按当前频道和当前标签重新加载数据。
        repository.refresh(currentChannel, selectedTag);
    }

    public void loadMore() {
        // 加载下一页数据。
        repository.loadMore();
    }

    public void selectTag(String tag) {
        // 如果点击的是已经选中的标签，就取消筛选。
        if (tag != null && tag.equals(selectedTag)) {
            selectedTag = null;
        } else {
            // 否则把点击的标签设置为当前筛选条件。
            selectedTag = tag;
        }

        // 标签变化后重新加载列表。
        repository.refresh(currentChannel, selectedTag);
    }

    public void openAd(AdItem item) {
        // 打开详情页前，先记录一次点击事件。
        repository.trackClick(item);
    }

    public void toggleLike(AdItem item) {
        // 切换点赞状态。
        repository.toggleLike(item);
    }

    public void toggleCollect(AdItem item) {
        // 切换收藏状态。
        repository.toggleCollected(item);
    }

    public void trackShare(AdItem item) {
        // 记录分享事件。
        repository.trackShare(item);
    }

    public void trackExposure(AdItem item) {
        // 记录曝光事件。
        repository.trackExposure(item);
    }

    public void ensureAiForAds(List<AdItem> ads) {
        // 对缺少 AI 摘要/标签的广告发起异步补全。
        repository.ensureAiForAds(ads);
    }

    public void saveScrollState(Parcelable state) {
        // 用“频道 + 标签”作为 key，保存当前列表滚动位置。
        scrollStates.put(key(), state);
    }

    public Parcelable consumeScrollState() {
        // 取出当前频道/标签对应的滚动位置，用于返回页面时恢复位置。
        return scrollStates.get(key());
    }

    private String key() {
        // 例如：精选::运动。没有标签时，后半部分为空。
        return currentChannel + "::" + (selectedTag == null ? "" : selectedTag);
    }
}

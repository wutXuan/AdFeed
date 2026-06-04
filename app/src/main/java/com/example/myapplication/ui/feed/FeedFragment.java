package com.example.myapplication.ui.feed;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentFeedBinding;
import com.example.myapplication.model.AdChannels;
import com.example.myapplication.model.AdItem;
import com.google.android.material.chip.Chip;
import com.google.android.material.tabs.TabLayout;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class FeedFragment extends Fragment {

    // fragment_feed.xml 对应的 ViewBinding。
    private FragmentFeedBinding binding;

    // 页面状态和业务操作入口。
    private FeedViewModel viewModel;

    // RecyclerView 的适配器，负责把广告数据变成卡片 View。
    private AdFeedAdapter adapter;

    // LinearLayoutManager 控制 RecyclerView 纵向列表布局。
    private LinearLayoutManager layoutManager;

    // Handler 用来延迟执行曝光检测。
    private final Handler handler = new Handler(Looper.getMainLooper());

    // 记录本轮页面生命周期里已经曝光过的广告，避免重复计数。
    private final Set<String> exposedIds = new HashSet<>();

    // 保存还没执行的曝光检测任务，方便滚动时取消旧任务。
    private Runnable pendingExposureCheck;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFeedBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 创建 ViewModel。Fragment 重建时，ViewModel 可以继续保存页面状态。
        viewModel = new ViewModelProvider(this).get(FeedViewModel.class);

        // 初始化顶部频道 Tab。
        setupTabs();

        // 初始化 RecyclerView 列表。
        setupRecycler();

        // 观察 ViewModel 暴露的数据。
        setupObservers();

        // 设置下拉刷新监听。
        binding.swipeRefresh.setOnRefreshListener(() -> {
            // 刷新时清空本轮曝光记录。
            exposedIds.clear();
            // 通知 ViewModel 重新加载当前频道。
            viewModel.refresh();
        });
    }

    private void setupTabs() {
        // 给 TabLayout 添加“精选 / 电商 / 本地”三个频道。
        for (String channel : AdChannels.all()) {
            binding.tabChannels.addTab(binding.tabChannels.newTab().setText(channel));
        }

        // 监听用户点击 Tab 的行为。
        binding.tabChannels.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // 切换频道前保存当前列表位置。
                saveScrollState();

                // 通知 ViewModel 切换频道。
                viewModel.selectChannel(String.valueOf(tab.getText()));

                // 新频道重新统计曝光。
                exposedIds.clear();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // 再次点击当前 Tab 时，列表平滑回到顶部。
                binding.recyclerAds.smoothScrollToPosition(0);
            }
        });
    }

    private void setupRecycler() {
        // 创建纵向线性布局管理器。
        layoutManager = new LinearLayoutManager(requireContext());

        // 创建广告列表适配器，并传入各种点击事件回调。
        adapter = new AdFeedAdapter(requireContext(), new AdFeedAdapter.Listener() {
            @Override
            public void onOpenAd(AdItem item) {
                // 进入详情前保存滚动位置。
                saveScrollState();

                // 记录广告点击。
                viewModel.openAd(item);

                // Bundle 用来把 adId 传给详情页。
                Bundle args = new Bundle();
                args.putString("adId", item.getId());

                // 通过 Navigation 跳转到详情页。
                NavHostFragment.findNavController(FeedFragment.this)
                        .navigate(R.id.action_feedFragment_to_adDetailFragment, args);
            }

            @Override
            public void onLike(AdItem item) {
                // 切换点赞状态。
                viewModel.toggleLike(item);
            }

            @Override
            public void onCollect(AdItem item) {
                // 切换收藏状态。
                viewModel.toggleCollect(item);
            }

            @Override
            public void onShare(AdItem item) {
                // 先记录分享事件。
                viewModel.trackShare(item);

                // 再调起系统分享面板。
                share(item);
            }

            @Override
            public void onTagClicked(String tag) {
                // 点击标签前保存当前位置。
                saveScrollState();

                // 用标签过滤列表。
                viewModel.selectTag(tag);

                // 筛选后回到顶部。
                binding.recyclerAds.scrollToPosition(0);
            }
        });

        // 把布局管理器设置给 RecyclerView。
        binding.recyclerAds.setLayoutManager(layoutManager);

        // 把适配器设置给 RecyclerView。
        binding.recyclerAds.setAdapter(adapter);

        // 设置复用池，RecyclerView 会把滑出屏幕的卡片复用起来。
        binding.recyclerAds.setRecycledViewPool(new RecyclerView.RecycledViewPool());

        // 监听滚动，用来做加载更多和曝光统计。
        binding.recyclerAds.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // 每次滚动都保存当前位置，避免刷新后跳回顶部。
                saveScrollState();
                // 快到底时加载更多。
                maybeLoadMore();
                // 滚动后延迟检查曝光。
                scheduleExposureCheck();
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                // 滚动停止时再检查一次曝光。
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    scheduleExposureCheck();
                }
            }
        });
    }

    private void setupObservers() {
        // 观察广告列表。只要 Repository 发出新列表，这里就会执行。
        viewModel.getAds().observe(getViewLifecycleOwner(), ads -> {
            // submitList 会把新列表交给 ListAdapter，DiffUtil 会自动计算局部刷新。
            adapter.submitList(ads, () -> {
                // 列表刷新完成后恢复滚动位置。
                restoreScrollState();
                // 恢复位置后检查曝光。
                scheduleExposureCheck();
            });

            // 给缺少 AI 信息的广告补摘要、标签和推荐理由。
            viewModel.ensureAiForAds(ads);

            // 没有广告时显示空状态文案。
            binding.textEmpty.setVisibility(ads == null || ads.isEmpty() ? View.VISIBLE : View.GONE);
        });

        // 观察下拉刷新状态，并同步到 SwipeRefreshLayout。
        viewModel.getRefreshing().observe(getViewLifecycleOwner(), refreshing ->
                binding.swipeRefresh.setRefreshing(Boolean.TRUE.equals(refreshing)));

        // 观察加载更多状态，并控制底部 ProgressBar 显示隐藏。
        viewModel.getLoadingMore().observe(getViewLifecycleOwner(), loading ->
                binding.progressLoadMore.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));

        // 观察当前频道可用标签，刷新标签筛选栏。
        viewModel.getTags().observe(getViewLifecycleOwner(), this::renderFilterChips);
    }

    private void renderFilterChips(List<String> tags) {
        // 先清空旧标签，避免重复添加。
        binding.chipGroupFilters.removeAllViews();

        // “全部”代表不使用标签筛选。
        addFilterChip("全部", viewModel.getSelectedTag() == null, null);

        // 如果标签列表为空，直接结束。
        if (tags == null) {
            return;
        }

        // 为每个标签创建一个 Chip。
        for (String tag : tags) {
            addFilterChip(tag, tag.equals(viewModel.getSelectedTag()), tag);
        }
    }

    private void addFilterChip(String text, boolean checked, String tagValue) {
        // Chip 是 Material 里的标签控件。
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setCheckable(true);
        chip.setChecked(checked);

        // 选中和未选中使用不同颜色。
        chip.setTextColor(ContextCompat.getColor(requireContext(), checked ? R.color.white : R.color.accent_blue));
        chip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),
                checked ? R.color.accent_blue : R.color.chip_bg)));

        // 点击 Chip 时切换筛选条件。
        chip.setOnClickListener(v -> {
            saveScrollState();
            viewModel.selectTag(tagValue);
            binding.recyclerAds.scrollToPosition(0);
        });

        // 把 Chip 加到 ChipGroup 里显示。
        binding.chipGroupFilters.addView(chip);
    }

    private void maybeLoadMore() {
        // 找到当前屏幕最后一个可见条目的位置。
        int lastVisible = layoutManager.findLastVisibleItemPosition();

        // 当最后可见条目接近列表底部时，触发加载更多。
        if (lastVisible >= adapter.getItemCount() - 2 && adapter.getItemCount() > 0) {
            viewModel.loadMore();
        }
    }

    private void scheduleExposureCheck() {
        // 如果已经有一个等待中的曝光任务，先取消，避免连续滚动时重复计算。
        if (pendingExposureCheck != null) {
            handler.removeCallbacks(pendingExposureCheck);
        }

        // 创建新的曝光检测任务。
        pendingExposureCheck = this::trackVisibleExposure;

        // 延迟 1 秒执行，表示广告需要停留一会儿才算曝光。
        handler.postDelayed(pendingExposureCheck, 1000);
    }

    private void trackVisibleExposure() {
        // 获取屏幕上第一个和最后一个可见条目的位置。
        int first = layoutManager.findFirstVisibleItemPosition();
        int last = layoutManager.findLastVisibleItemPosition();

        // 遍历当前可见范围内的所有广告。
        for (int i = first; i <= last; i++) {
            View child = layoutManager.findViewByPosition(i);
            AdItem item = adapter.getItemAt(i);

            // 找不到 View、找不到数据、或者已经曝光过，就跳过。
            if (child == null || item == null || exposedIds.contains(item.getId())) {
                continue;
            }

            // 计算这个卡片在 RecyclerView 内实际可见的高度。
            int visibleHeight = Math.min(child.getBottom(), binding.recyclerAds.getHeight()) - Math.max(child.getTop(), 0);

            // 可见高度超过卡片高度一半，就认为曝光成功。
            if (visibleHeight >= child.getHeight() / 2) {
                exposedIds.add(item.getId());
                viewModel.trackExposure(item);
            }
        }
    }

    private void saveScrollState() {
        // 只有 layoutManager 已经创建后才能保存位置。
        if (layoutManager != null) {
            Parcelable state = layoutManager.onSaveInstanceState();
            viewModel.saveScrollState(state);
        }
    }

    private void restoreScrollState() {
        // 从 ViewModel 取出当前频道/标签对应的滚动状态。
        Parcelable state = viewModel.consumeScrollState();

        // 有状态就恢复，没有就保持默认位置。
        if (state != null) {
            layoutManager.onRestoreInstanceState(state);
        }
    }

    private void share(AdItem item) {
        // ACTION_SEND 是系统分享 Intent。
        Intent intent = new Intent(Intent.ACTION_SEND);

        // 分享的是纯文本。
        intent.setType("text/plain");

        // 分享内容由广告标题和描述组成。
        intent.putExtra(Intent.EXTRA_TEXT, item.getTitle() + "\n" + item.getDescription());

        // 打开系统分享选择器。
        startActivity(Intent.createChooser(intent, "分享广告"));
    }

    @Override
    public void onDestroyView() {
        // Fragment 销毁 View 时，取消还没执行的曝光检测任务。
        if (pendingExposureCheck != null) {
            handler.removeCallbacks(pendingExposureCheck);
        }

        // 解绑 Adapter，避免 Fragment View 销毁后 RecyclerView 还持有旧引用。
        binding.recyclerAds.setAdapter(null);

        // ViewBinding 置空，避免内存泄漏。
        binding = null;
        super.onDestroyView();
    }
}

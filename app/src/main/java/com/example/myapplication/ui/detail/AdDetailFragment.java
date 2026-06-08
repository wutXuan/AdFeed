package com.example.myapplication.ui.detail;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;

import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentAdDetailBinding;
import com.example.myapplication.model.AdItem;
import com.example.myapplication.ui.common.AdImageLoader;
import com.google.android.material.chip.Chip;

public class AdDetailFragment extends Fragment {
    private FragmentAdDetailBinding binding;
    private AdDetailViewModel viewModel;
    private ExoPlayer player;
    private String currentVideoId;
    private boolean muted = true;
    private AdItem currentItem;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdDetailViewModel.class);
        String adId = requireArguments().getString("adId");
        viewModel.load(adId);
        viewModel.getAd().observe(getViewLifecycleOwner(), this::bind);
        binding.buttonDetailMute.setOnClickListener(v -> toggleMute());
    }

    private void bind(AdItem item) {
        currentItem = item;
        binding.textBrand.setText(item.getBrand());
        binding.textTitle.setText(item.getTitle());
        binding.textSummary.setText(isBlank(item.getSummary()) ? item.getDescription() : item.getSummary());
        binding.textReason.setText(isBlank(item.getAiReason()) ? "AI 正在整理推荐理由，主流程不会被它卡住。" : item.getAiReason());
        binding.textPlayful.setText(isBlank(item.getPlayfulCopy()) ? "不硬广模式：先把有用的点讲清楚。" : item.getPlayfulCopy());
        binding.textDescription.setText(item.getDescription());
        binding.textStats.setText("曝光 " + item.getExposureCount() + " · 点击 " + item.getClickCount() + " · 点赞 " + item.getLikeCount());
        binding.buttonLike.setText(item.isLiked() ? "已赞 " + item.getLikeCount() : "赞 " + item.getLikeCount());
        binding.buttonCollect.setText(item.isCollected() ? "已收藏" : "收藏");
        binding.buttonShare.setText("分享");
        binding.buttonLike.setOnClickListener(v -> {
            v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(90)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).start())
                    .start();
            viewModel.toggleLike(item);
        });
        binding.buttonCollect.setOnClickListener(v -> viewModel.toggleCollect(item));
        binding.buttonShare.setOnClickListener(v -> {
            viewModel.trackShare(item);
            share(item);
        });
        renderTags(item);
        if (AdItem.TYPE_VIDEO.equals(item.getType())) {
            showVideo(item);
        } else {
            releasePlayer();
            binding.playerDetail.setVisibility(View.GONE);
            binding.buttonDetailMute.setVisibility(View.GONE);
            binding.imageDetail.setVisibility(View.VISIBLE);
            AdImageLoader.load(binding.imageDetail, item);
        }
    }

    private void showVideo(AdItem item) {
        AdImageLoader.load(binding.imageDetail, item);
        binding.imageDetail.setVisibility(View.VISIBLE);
        binding.playerDetail.setVisibility(View.GONE);
        binding.buttonDetailMute.setVisibility(View.VISIBLE);
        if (player == null || !item.getId().equals(currentVideoId)) {
            releasePlayer();
            player = new ExoPlayer.Builder(requireContext()).build();
            player.setMediaItem(MediaItem.fromUri(item.getMediaUrl()));
            player.setVolume(muted ? 0f : 1f);
            player.prepare();
            player.setPlayWhenReady(true);
            binding.playerDetail.setPlayer(player);
            currentVideoId = item.getId();
        }
        binding.buttonDetailMute.setText(muted ? "静音" : "有声");
    }

    private void renderTags(AdItem item) {
        binding.chipGroupTags.removeAllViews();
        int textColor = ContextCompat.getColor(requireContext(), R.color.accent_blue);
        int bgColor = ContextCompat.getColor(requireContext(), R.color.chip_bg);
        for (String tag : item.getTags()) {
            Chip chip = new Chip(requireContext());
            chip.setText(tag);
            chip.setTextSize(12f);
            chip.setCheckable(false);
            chip.setChipBackgroundColor(ColorStateList.valueOf(bgColor));
            chip.setTextColor(textColor);
            binding.chipGroupTags.addView(chip);
        }
    }

    private void toggleMute() {
        muted = !muted;
        if (player != null) {
            player.setVolume(muted ? 0f : 1f);
        }
        binding.buttonDetailMute.setText(muted ? "静音" : "有声");
    }

    private void share(AdItem item) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, item.getTitle() + "\n" + item.getDescription());
        startActivity(Intent.createChooser(intent, "分享广告"));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    public void onDestroyView() {
        releasePlayer();
        binding = null;
        super.onDestroyView();
    }

    private void releasePlayer() {
        if (player != null) {
            binding.playerDetail.setPlayer(null);
            player.release();
            player = null;
            currentVideoId = null;
        }
    }
}

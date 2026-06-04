package com.example.myapplication.ui.feed;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.media.PlayerPool;
import com.example.myapplication.model.AdItem;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class AdFeedAdapter extends ListAdapter<AdItem, RecyclerView.ViewHolder> {
    public interface Listener {
        void onOpenAd(AdItem item);

        void onLike(AdItem item);

        void onCollect(AdItem item);

        void onShare(AdItem item);

        void onTagClicked(String tag);
    }

    private static final int TYPE_LARGE = 1;
    private static final int TYPE_SMALL = 2;
    private static final int TYPE_VIDEO = 3;

    private final Listener listener;
    private final PlayerPool playerPool;

    public AdFeedAdapter(Context context, Listener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.playerPool = PlayerPool.getInstance(context);
    }

    @Override
    public int getItemViewType(int position) {
        AdItem item = getItem(position);
        if (AdItem.TYPE_VIDEO.equals(item.getType())) {
            return TYPE_VIDEO;
        }
        if (AdItem.TYPE_SMALL_IMAGE.equals(item.getType())) {
            return TYPE_SMALL;
        }
        return TYPE_LARGE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_VIDEO) {
            return new VideoHolder(inflater.inflate(R.layout.item_ad_video, parent, false), listener, playerPool);
        }
        if (viewType == TYPE_SMALL) {
            return new BaseHolder(inflater.inflate(R.layout.item_ad_small_image, parent, false), listener);
        }
        return new BaseHolder(inflater.inflate(R.layout.item_ad_large_image, parent, false), listener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AdItem item = getItem(position);
        if (holder instanceof VideoHolder) {
            ((VideoHolder) holder).bind(item);
        } else {
            ((BaseHolder) holder).bind(item);
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof VideoHolder) {
            ((VideoHolder) holder).recycle();
        }
        super.onViewRecycled(holder);
    }

    public AdItem getItemAt(int position) {
        if (position < 0 || position >= getItemCount()) {
            return null;
        }
        return getItem(position);
    }

    static class BaseHolder extends RecyclerView.ViewHolder {
        protected final MaterialCardView cardRoot;
        protected final ImageView imageMedia;
        protected final TextView textBrand;
        protected final TextView textTitle;
        protected final TextView textSummary;
        protected final TextView textStats;
        protected final ChipGroup chipGroupTags;
        protected final MaterialButton buttonLike;
        protected final MaterialButton buttonCollect;
        protected final MaterialButton buttonShare;
        protected final Listener listener;

        BaseHolder(@NonNull View itemView, Listener listener) {
            super(itemView);
            this.listener = listener;
            cardRoot = itemView.findViewById(R.id.card_root);
            imageMedia = itemView.findViewById(R.id.image_media);
            textBrand = itemView.findViewById(R.id.text_brand);
            textTitle = itemView.findViewById(R.id.text_title);
            textSummary = itemView.findViewById(R.id.text_summary);
            textStats = itemView.findViewById(R.id.text_stats);
            chipGroupTags = itemView.findViewById(R.id.chip_group_tags);
            buttonLike = itemView.findViewById(R.id.button_like);
            buttonCollect = itemView.findViewById(R.id.button_collect);
            buttonShare = itemView.findViewById(R.id.button_share);
        }

        void bind(AdItem item) {
            textBrand.setText(item.getBrand());
            textTitle.setText(item.getTitle());
            textSummary.setText(summary(item));
            textStats.setText("曝光 " + item.getExposureCount() + " · 点击 " + item.getClickCount());
            buttonLike.setText(item.isLiked() ? "已赞 " + item.getLikeCount() : "赞 " + item.getLikeCount());
            buttonCollect.setText(item.isCollected() ? "已收藏" : "收藏");
            buttonShare.setText("分享");
            buttonLike.setSelected(item.isLiked());
            buttonCollect.setSelected(item.isCollected());
            Glide.with(imageMedia)
                    .load(item.getThumbnailUrl())
                    .centerCrop()
                    .placeholder(android.R.color.darker_gray)
                    .error(android.R.color.darker_gray)
                    .into(imageMedia);
            renderTags(chipGroupTags, item, listener);
            cardRoot.setOnClickListener(v -> listener.onOpenAd(item));
            buttonLike.setOnClickListener(v -> {
                animatePop(buttonLike);
                listener.onLike(item);
            });
            buttonCollect.setOnClickListener(v -> listener.onCollect(item));
            buttonShare.setOnClickListener(v -> listener.onShare(item));
        }

        protected String summary(AdItem item) {
            if (item.getSummary() == null || item.getSummary().trim().isEmpty()) {
                return item.getDescription();
            }
            return item.getSummary();
        }

        protected void renderTags(ChipGroup chipGroup, AdItem item, Listener listener) {
            chipGroup.removeAllViews();
            Context context = chipGroup.getContext();
            int textColor = ContextCompat.getColor(context, R.color.accent_blue);
            int bgColor = ContextCompat.getColor(context, R.color.chip_bg);
            for (String tag : item.getTags()) {
                Chip chip = new Chip(context);
                chip.setText(tag);
                chip.setTextSize(12f);
                chip.setCheckable(false);
                chip.setChipBackgroundColor(ColorStateList.valueOf(bgColor));
                chip.setTextColor(textColor);
                chip.setOnClickListener(v -> listener.onTagClicked(tag));
                chipGroup.addView(chip);
            }
        }

        protected void animatePop(View view) {
            view.animate()
                    .scaleX(1.08f)
                    .scaleY(1.08f)
                    .setDuration(90)
                    .withEndAction(() -> view.animate().scaleX(1f).scaleY(1f).setDuration(120).start())
                    .start();
        }
    }

    static class VideoHolder extends BaseHolder {
        private final PlayerView playerView;
        private final MaterialButton buttonPlay;
        private final MaterialButton buttonMute;
        private final PlayerPool playerPool;
        private String activeId;
        private boolean playing;

        VideoHolder(@NonNull View itemView, Listener listener, PlayerPool playerPool) {
            super(itemView, listener);
            this.playerPool = playerPool;
            playerView = itemView.findViewById(R.id.player_view);
            buttonPlay = itemView.findViewById(R.id.button_play_video);
            buttonMute = itemView.findViewById(R.id.button_mute);
        }

        @Override
        void bind(AdItem item) {
            recycle();
            super.bind(item);
            activeId = item.getId();
            playing = false;
            playerView.setVisibility(View.GONE);
            imageMedia.setVisibility(View.VISIBLE);
            buttonPlay.setText("播放");
            buttonMute.setText(playerPool.isMuted() ? "静音" : "有声");
            buttonPlay.setOnClickListener(v -> togglePlay(item));
            buttonMute.setOnClickListener(v -> buttonMute.setText(playerPool.toggleMuted() ? "静音" : "有声"));
        }

        private void togglePlay(AdItem item) {
            if (playing) {
                playerPool.release(item.getId());
                playerView.setPlayer(null);
                imageMedia.setVisibility(View.VISIBLE);
                playerView.setVisibility(View.GONE);
                buttonPlay.setText("播放");
                playing = false;
                return;
            }
            ExoPlayer player = playerPool.acquire(item.getId(), item.getMediaUrl());
            playerView.setPlayer(player);
            imageMedia.setVisibility(View.GONE);
            playerView.setVisibility(View.VISIBLE);
            player.setPlayWhenReady(true);
            buttonPlay.setText("暂停");
            playing = true;
        }

        void recycle() {
            if (activeId != null) {
                playerPool.release(activeId);
                activeId = null;
            }
            if (playerView != null) {
                playerView.setPlayer(null);
            }
            playing = false;
        }
    }

    private static final DiffUtil.ItemCallback<AdItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<AdItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull AdItem oldItem, @NonNull AdItem newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull AdItem oldItem, @NonNull AdItem newItem) {
            return oldItem.equals(newItem);
        }
    };
}

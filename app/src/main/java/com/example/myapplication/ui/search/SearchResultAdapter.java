package com.example.myapplication.ui.search;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.ai.SearchResult;
import com.google.android.material.card.MaterialCardView;

public class SearchResultAdapter extends ListAdapter<SearchResult, SearchResultAdapter.ResultHolder> {
    public interface Listener {
        void onResultClicked(SearchResult result);
    }

    private final Listener listener;

    public SearchResultAdapter(Listener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ResultHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ResultHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_result, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ResultHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ResultHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardRoot;
        private final TextView textTitle;
        private final TextView textReason;
        private final TextView textTags;

        ResultHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_root);
            textTitle = itemView.findViewById(R.id.text_title);
            textReason = itemView.findViewById(R.id.text_reason);
            textTags = itemView.findViewById(R.id.text_tags);
        }

        void bind(SearchResult result, Listener listener) {
            textTitle.setText(result.getAdItem().getTitle());
            textReason.setText(result.getReason());
            textTags.setText(TextUtils.join(" / ", result.getMatchedTags()));
            cardRoot.setOnClickListener(v -> listener.onResultClicked(result));
        }
    }

    private static final DiffUtil.ItemCallback<SearchResult> DIFF_CALLBACK = new DiffUtil.ItemCallback<SearchResult>() {
        @Override
        public boolean areItemsTheSame(@NonNull SearchResult oldItem, @NonNull SearchResult newItem) {
            return oldItem.getAdItem().getId().equals(newItem.getAdItem().getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull SearchResult oldItem, @NonNull SearchResult newItem) {
            return oldItem.getReason().equals(newItem.getReason())
                    && oldItem.getMatchedTags().equals(newItem.getMatchedTags());
        }
    };
}

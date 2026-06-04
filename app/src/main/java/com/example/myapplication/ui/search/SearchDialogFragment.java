package com.example.myapplication.ui.search;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.R;
import com.example.myapplication.data.AdRepository;
import com.example.myapplication.databinding.DialogSearchBinding;

public class SearchDialogFragment extends DialogFragment {
    private DialogSearchBinding binding;
    private SearchResultAdapter adapter;
    private AdRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repository = AdRepository.getInstance(requireContext());
        adapter = new SearchResultAdapter(result -> {
            Bundle args = new Bundle();
            args.putString("adId", result.getAdItem().getId());
            dismiss();
            NavHostFragment navHostFragment = (NavHostFragment) requireActivity()
                    .getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment_content_main);
            if (navHostFragment != null) {
                navHostFragment.getNavController().navigate(R.id.action_global_adDetailFragment, args);
            }
        });
        binding.recyclerSearchResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerSearchResults.setAdapter(adapter);
        binding.buttonSearch.setOnClickListener(v -> search());
        binding.editQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search();
                return true;
            }
            return false;
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            }
        }
    }

    private void search() {
        String query = binding.editQuery.getText() == null ? "" : binding.editQuery.getText().toString().trim();
        if (query.isEmpty()) {
            binding.textSearchState.setText("先描述一下想看的广告，例如：适合学生党的运动装备。");
            return;
        }
        binding.progressSearch.setVisibility(View.VISIBLE);
        binding.textSearchState.setText("正在理解你的描述...");
        repository.searchAllAds(query, results -> {
            if (binding == null) {
                return;
            }
            binding.progressSearch.setVisibility(View.GONE);
            adapter.submitList(results);
            binding.textSearchState.setText(results.isEmpty() ? "没有找到合适结果，换个说法试试。" : "找到 " + results.size() + " 条匹配广告");
        });
    }

    @Override
    public void onDestroyView() {
        binding.recyclerSearchResults.setAdapter(null);
        binding = null;
        super.onDestroyView();
    }
}

package com.example.myapplication.ui.detail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.myapplication.data.AdRepository;
import com.example.myapplication.model.AdItem;

public class AdDetailViewModel extends AndroidViewModel {
    private final AdRepository repository;
    private final MutableLiveData<String> adId = new MutableLiveData<>();
    private final LiveData<AdItem> ad;

    public AdDetailViewModel(@NonNull Application application) {
        super(application);
        repository = AdRepository.getInstance(application);
        ad = Transformations.switchMap(adId, repository::observeAd);
    }

    public LiveData<AdItem> getAd() {
        return ad;
    }

    public void load(String id) {
        adId.setValue(id);
    }

    public void toggleLike(AdItem item) {
        repository.toggleLike(item);
    }

    public void toggleCollect(AdItem item) {
        repository.toggleCollected(item);
    }

    public void trackShare(AdItem item) {
        repository.trackShare(item);
    }
}

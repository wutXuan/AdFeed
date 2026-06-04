package com.example.myapplication.ai;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface LlmApiService {
    @POST("v1/chat/completions")
    Call<LlmModels.ChatCompletionResponse> chatCompletion(
            @Header("Authorization") String authorization,
            @Body LlmModels.ChatCompletionRequest request
    );
}

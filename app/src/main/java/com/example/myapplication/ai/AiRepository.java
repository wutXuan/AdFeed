package com.example.myapplication.ai;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.example.myapplication.BuildConfig;
import com.example.myapplication.model.AdItem;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AiRepository {
    private static final String TAG = "AiRepository";
    private static final int MAX_RETRY_COUNT = 3;
    private static final long DEAD_LETTER_ACK_DELAY_MS = 60_000L;

    public interface ResultCallback<T> {
        void onSuccess(T result);

        void onError(Throwable throwable);
    }

    private static volatile AiRepository instance;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final ScheduledExecutorService queueExecutor = Executors.newSingleThreadScheduledExecutor();
    private final Object queueLock = new Object();
    private final Queue<AiQueueTask<?>> messageQueue = new ArrayDeque<>();
    private final Map<String, AiQueueTask<?>> deadLetterQueue = new HashMap<>();
    private final AtomicInteger taskSequence = new AtomicInteger();
    private final Gson gson = new Gson();
    private final LlmApiService service;
    private final String apiKey;
    private final String model;
    private boolean queueRunning;

    private AiRepository() {
        apiKey = BuildConfig.LLM_API_KEY == null ? "" : BuildConfig.LLM_API_KEY.trim();
        model = TextUtils.isEmpty(BuildConfig.LLM_MODEL) ? "gpt-4o-mini" : BuildConfig.LLM_MODEL;
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BASIC : HttpLoggingInterceptor.Level.NONE);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ensureTrailingSlash(BuildConfig.LLM_BASE_URL))
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        service = retrofit.create(LlmApiService.class);
    }

    public static AiRepository getInstance() {
        if (instance == null) {
            synchronized (AiRepository.class) {
                if (instance == null) {
                    instance = new AiRepository();
                }
            }
        }
        return instance;
    }

    public void generateAdMeta(AdItem adItem, ResultCallback<AiMeta> callback) {
        Log.d(TAG, "哈哈哈哈1111111");
        if (TextUtils.isEmpty(apiKey)) {
            executor.execute(() -> postSuccess(callback, fallbackMeta(adItem)));
            return;
        }
        LlmModels.ChatCompletionRequest request = new LlmModels.ChatCompletionRequest();
        request.model = model;
        request.messages.add(new LlmModels.Message("system",
                "你是广告内容策略师。只返回 JSON，不要 markdown。字段为 summary、tags、reason、playfulCopy。tags 是 3 到 5 个中文短标签。"));
        request.messages.add(new LlmModels.Message("user",
                "为这条广告生成更容易被用户接受的信息流内容。\n"
                        + "标题：" + adItem.getTitle() + "\n"
                        + "品牌：" + adItem.getBrand() + "\n"
                        + "描述：" + adItem.getDescription() + "\n"
                        + "已有标签：" + TextUtils.join(",", adItem.getTags())));
        enqueueAiTask("generateAdMeta", request, content -> parseMeta(content, adItem), callback);
    }

    public void searchAds(String message, List<AdItem> candidates, ResultCallback<List<SearchResult>> callback) {
        if (TextUtils.isEmpty(apiKey)) {
            executor.execute(() -> postSuccess(callback, fallbackSearch(message, candidates)));
            return;
        }
        List<AdItem> compactCandidates = prefilter(message, candidates, 16);
        LlmModels.ChatCompletionRequest request = new LlmModels.ChatCompletionRequest();
        request.model = model;
        request.messages.add(new LlmModels.Message("system",
                "你是广告搜索排序助手。只返回 JSON，不要 markdown。字段 results 是数组，每项包含 id、reason、matchedTags。"));
        request.messages.add(new LlmModels.Message("user",
                "用户想看：" + message + "\n候选广告：" + toCandidateJson(compactCandidates)
                        + "\n请返回最匹配的 1 到 8 条广告。"));
        enqueueAiTask("searchAds", request, content -> parseSearch(content, compactCandidates), callback);
    }

    private <T> void enqueueAiTask(String type,
                                   LlmModels.ChatCompletionRequest request,
                                   AiResponseParser<T> parser,
                                   ResultCallback<T> callback) {
        AiQueueTask<T> task = new AiQueueTask<>(
                "ai-" + taskSequence.incrementAndGet(),
                type,
                request,
                parser,
                callback
        );
        synchronized (queueLock) {
            messageQueue.offer(task);
            Log.d(TAG, "mq enqueue id=" + task.id + " type=" + task.type + " size=" + messageQueue.size());
            if (!queueRunning) {
                queueRunning = true;
                queueExecutor.execute(this::consumeNextTask);
            }
        }
    }

    private void consumeNextTask() {
        AiQueueTask<?> task;
        synchronized (queueLock) {
            task = messageQueue.poll();
            if (task == null) {
                queueRunning = false;
                return;
            }
        }
        Log.d(TAG, "mq consume id=" + task.id + " type=" + task.type + " attempt=" + (task.retryCount + 1));
        runAiTask(task);
    }

    private <T> void runAiTask(AiQueueTask<T> task) {
        service.chatCompletion("Bearer " + apiKey, task.request).enqueue(new Callback<LlmModels.ChatCompletionResponse>() {
            @Override
            public void onResponse(Call<LlmModels.ChatCompletionResponse> call, Response<LlmModels.ChatCompletionResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    retryOrDeadLetter(task, new IllegalStateException("AI response invalid, code=" + response.code()));
                    return;
                }
                try {
                    T result = task.parser.parse(response.body().firstContent());
                    Log.d(TAG, "mq success id=" + task.id + " type=" + task.type);
                    postSuccess(task.callback, result);
                    scheduleNextTask();
                } catch (RuntimeException exception) {
                    retryOrDeadLetter(task, exception);
                }
            }

            @Override
            public void onFailure(Call<LlmModels.ChatCompletionResponse> call, Throwable throwable) {
                retryOrDeadLetter(task, throwable);
            }
        });
    }

    private void retryOrDeadLetter(AiQueueTask<?> task, Throwable throwable) {
        if (task.retryCount < MAX_RETRY_COUNT) {
            task.retryCount++;
            synchronized (queueLock) {
                messageQueue.offer(task);
                Log.w(TAG, "mq retry id=" + task.id
                        + " type=" + task.type
                        + " retry=" + task.retryCount + "/" + MAX_RETRY_COUNT
                        + " size=" + messageQueue.size()
                        + " cause=" + describe(throwable));
            }
        } else {
            moveToDeadLetter(task, throwable);
        }
        scheduleNextTask();
    }

    private void moveToDeadLetter(AiQueueTask<?> task, Throwable throwable) {
        synchronized (queueLock) {
            deadLetterQueue.put(task.id, task);
        }
        Log.w(TAG, "mq dead-letter id=" + task.id
                + " type=" + task.type
                + " attempts=" + (task.retryCount + 1)
                + " cause=" + describe(throwable));
        queueExecutor.schedule(() -> ackDeadLetter(task), DEAD_LETTER_ACK_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void ackDeadLetter(AiQueueTask<?> task) {
        boolean removed;
        synchronized (queueLock) {
            removed = deadLetterQueue.remove(task.id) != null;
        }
        if (removed) {
            Log.d(TAG, "mq xack dead-letter id=" + task.id + " type=" + task.type);
        }
    }

    private void scheduleNextTask() {
        queueExecutor.execute(this::consumeNextTask);
    }

    private String describe(Throwable throwable) {
        if (throwable == null) {
            return "unknown";
        }
        String message = throwable.getMessage();
        return throwable.getClass().getSimpleName() + (TextUtils.isEmpty(message) ? "" : ": " + message);
    }

    private AiMeta parseMeta(String content, AdItem adItem) {
        JsonObject object = JsonParser.parseString(cleanJson(content)).getAsJsonObject();
        String summary = getString(object, "summary", fallbackSummary(adItem));
        String reason = getString(object, "reason", "它和你当前的兴趣标签很贴近。");
        String playfulCopy = getString(object, "playfulCopy", "这不是硬广，是一个顺手变好的生活小开关。");
        List<String> tags = getStringArray(object, "tags");
        if (tags.isEmpty()) {
            tags = adItem.getTags();
        }
        return new AiMeta(summary, tags, reason, playfulCopy);
    }

    private List<SearchResult> parseSearch(String content, List<AdItem> candidates) {
        Map<String, AdItem> byId = new HashMap<>();
        for (AdItem item : candidates) {
            byId.put(item.getId(), item);
        }
        JsonObject object = JsonParser.parseString(cleanJson(content)).getAsJsonObject();
        JsonArray results = object.has("results") && object.get("results").isJsonArray()
                ? object.getAsJsonArray("results") : new JsonArray();
        List<SearchResult> parsed = new ArrayList<>();
        for (JsonElement element : results) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject item = element.getAsJsonObject();
            String id = getString(item, "id", "");
            AdItem adItem = byId.get(id);
            if (adItem == null) {
                continue;
            }
            parsed.add(new SearchResult(adItem,
                    getString(item, "reason", "匹配你的描述"),
                    getStringArray(item, "matchedTags")));
        }
        if (parsed.isEmpty()) {
            return fallbackSearch("", candidates);
        }
        return parsed;
    }

    private List<SearchResult> fallbackSearch(String message, List<AdItem> candidates) {
        List<ScoredAd> scored = new ArrayList<>();
        for (AdItem item : candidates) {
            int score = score(message, item);
            if (score > 0) {
                scored.add(new ScoredAd(item, score));
            }
        }
        if (scored.isEmpty()) {
            for (int i = 0; i < Math.min(6, candidates.size()); i++) {
                scored.add(new ScoredAd(candidates.get(i), 1));
            }
        }
        Collections.sort(scored, (left, right) -> Integer.compare(right.score, left.score));
        List<SearchResult> results = new ArrayList<>();
        for (int i = 0; i < Math.min(8, scored.size()); i++) {
            AdItem item = scored.get(i).item;
            results.add(new SearchResult(item, "根据标题、标签和广告描述为你本地匹配。", item.getTags()));
        }
        return results;
    }

    private List<AdItem> prefilter(String message, List<AdItem> candidates, int limit) {
        List<ScoredAd> scored = new ArrayList<>();
        for (AdItem item : candidates) {
            scored.add(new ScoredAd(item, Math.max(1, score(message, item))));
        }
        Collections.sort(scored, (left, right) -> Integer.compare(right.score, left.score));
        List<AdItem> result = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, scored.size()); i++) {
            result.add(scored.get(i).item);
        }
        return result;
    }

    private int score(String message, AdItem item) {
        if (TextUtils.isEmpty(message)) {
            return 1;
        }
        String query = message.toLowerCase(Locale.ROOT);
        int score = 0;
        score += containsScore(query, item.getTitle(), 6);
        score += containsScore(query, item.getBrand(), 3);
        score += containsScore(query, item.getDescription(), 2);
        score += containsScore(query, item.getSummary(), 2);
        for (String tag : item.getTags()) {
            score += containsScore(query, tag, 8);
        }
        return score;
    }

    private int containsScore(String query, String value, int weight) {
        if (TextUtils.isEmpty(value)) {
            return 0;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (query.contains(lower) || lower.contains(query)) {
            return weight;
        }
        int fuzzy = 0;
        for (int i = 0; i < query.length(); i++) {
            if (lower.indexOf(query.charAt(i)) >= 0) {
                fuzzy++;
            }
        }
        return fuzzy >= Math.min(2, query.length()) ? 1 : 0;
    }

    private AiMeta fallbackMeta(AdItem adItem) {
        List<String> tags = new ArrayList<>(adItem.getTags());
        if (tags.isEmpty()) {
            tags.add("精选");
            tags.add("AI推荐");
        }
        Set<String> unique = new HashSet<>(tags);
        if (unique.size() < 4) {
            tags.add("不硬广");
        }
        return new AiMeta(
                fallbackSummary(adItem),
                tags,
                "根据「" + TextUtils.join(" / ", tags) + "」判断，它适合正在寻找轻量选择的用户。",
                "把广告翻译成人话：这是一件可能马上派上用场的小东西。"
        );
    }

    private String fallbackSummary(AdItem adItem) {
        return adItem.getBrand() + "：用一个更轻松的理由，把「" + adItem.getTitle() + "」放进今天。";
    }

    private String toCandidateJson(List<AdItem> candidates) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AdItem item : candidates) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", item.getId());
            row.put("title", item.getTitle());
            row.put("brand", item.getBrand());
            row.put("description", item.getDescription());
            row.put("summary", item.getSummary());
            row.put("tags", item.getTags());
            rows.add(row);
        }
        return gson.toJson(rows);
    }

    private List<String> getStringArray(JsonObject object, String key) {
        if (!object.has(key) || !object.get(key).isJsonArray()) {
            return new ArrayList<>();
        }
        List<String> values = new ArrayList<>();
        for (JsonElement element : object.getAsJsonArray(key)) {
            if (!element.isJsonNull()) {
                String value = element.getAsString();
                if (!TextUtils.isEmpty(value)) {
                    values.add(value);
                }
            }
        }
        return values;
    }

    private String getString(JsonObject object, String key, String fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        String value = object.get(key).getAsString();
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private String cleanJson(String content) {
        String cleaned = content == null ? "" : content.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }
        return cleaned;
    }

    private <T> void postSuccess(ResultCallback<T> callback, T value) {
        mainHandler.post(() -> callback.onSuccess(value));
    }

    private static String ensureTrailingSlash(String baseUrl) {
        if (TextUtils.isEmpty(baseUrl)) {
            return "https://api.openai.com/";
        }
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    private interface AiResponseParser<T> {
        T parse(String content);
    }

    private static class AiQueueTask<T> {
        final String id;
        final String type;
        final LlmModels.ChatCompletionRequest request;
        final AiResponseParser<T> parser;
        final ResultCallback<T> callback;
        int retryCount;

        AiQueueTask(String id,
                    String type,
                    LlmModels.ChatCompletionRequest request,
                    AiResponseParser<T> parser,
                    ResultCallback<T> callback) {
            this.id = id;
            this.type = type;
            this.request = request;
            this.parser = parser;
            this.callback = callback;
        }
    }

    private static class ScoredAd {
        final AdItem item;
        final int score;

        ScoredAd(AdItem item, int score) {
            this.item = item;
            this.score = score;
        }
    }
}

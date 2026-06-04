package com.example.myapplication.ai;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public final class LlmModels {
    private LlmModels() {
    }

    public static class ChatCompletionRequest {
        @SerializedName("model")
        public String model;
        @SerializedName("messages")
        public List<Message> messages = new ArrayList<>();
        @SerializedName("temperature")
        public double temperature = 0.35;
        @SerializedName("response_format")
        public ResponseFormat responseFormat = new ResponseFormat("json_object");
    }

    public static class Message {
        @SerializedName("role")
        public String role;
        @SerializedName("content")
        public String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public static class ResponseFormat {
        @SerializedName("type")
        public String type;

        public ResponseFormat(String type) {
            this.type = type;
        }
    }

    public static class ChatCompletionResponse {
        @SerializedName("choices")
        public List<Choice> choices;

        public String firstContent() {
            if (choices == null || choices.isEmpty() || choices.get(0).message == null) {
                return "";
            }
            return choices.get(0).message.content == null ? "" : choices.get(0).message.content;
        }
    }

    public static class Choice {
        @SerializedName("message")
        public ResponseMessage message;
    }

    public static class ResponseMessage {
        @SerializedName("content")
        public String content;
    }
}

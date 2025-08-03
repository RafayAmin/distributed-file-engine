// src/main/java/com/rafay/fileengine/common/MessageUtils.java
package com.rafay.fileengine.common;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import java.util.Map;

public class MessageUtils {
    private static final Gson gson = new Gson();

    // Create an INDEX request
    public static String createIndexRequest(String clientId, String filePath, Map<String, Integer> wordFreqs) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "INDEX");
        json.addProperty("client_id", clientId);
        json.addProperty("file_path", filePath);
        json.add("words", gson.toJsonTree(wordFreqs));
        return gson.toJson(json);
    }

    // Create a SEARCH request
    public static String createSearchRequest(String... terms) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "SEARCH");
        JsonArray query = new JsonArray();
        for (String term : terms) {
            query.add(term);
        }
        json.add("query", query);
        return gson.toJson(json);
    }

    // Parse a message to get its type
    public static String getMessageType(String message) {
        JsonObject json = gson.fromJson(message, JsonObject.class);
        return json.get("type").getAsString();
    }

    // Get client_id from an INDEX message
    public static String getClientId(String message) {
        JsonObject json = gson.fromJson(message, JsonObject.class);
        return json.get("client_id").getAsString();
    }

    // Get file_path from an INDEX message
    public static String getFilePath(String message) {
        JsonObject json = gson.fromJson(message, JsonObject.class);
        return json.get("file_path").getAsString();
    }

    // Get words map from an INDEX message
    public static Map<String, Integer> getWordFrequencies(String message) {
        JsonObject json = gson.fromJson(message, JsonObject.class);
        return gson.fromJson(json.get("words"), Map.class);
    }

    // Get query terms from a SEARCH message
    public static String[] getQueryTerms(String message) {
        JsonObject json = gson.fromJson(message, JsonObject.class);
        JsonArray query = json.getAsJsonArray("query");
        String[] terms = new String[query.size()];
        for (int i = 0; i < query.size(); i++) {
            terms[i] = query.get(i).getAsString();
        }
        return terms;
    }

    // Create an INDEX_REPLY
    public static String createIndexReply(String status, String message) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "INDEX_REPLY");
        json.addProperty("status", status);
        json.addProperty("message", message);
        return gson.toJson(json);
    }

    // Create a SEARCH_REPLY
    public static String createSearchReply(Map<String, Integer> results) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "SEARCH_REPLY");
        JsonObject resultsJson = new JsonObject();
        for (Map.Entry<String, Integer> entry : results.entrySet()) {
            resultsJson.addProperty(entry.getKey(), entry.getValue());
        }
        json.add("results", resultsJson);
        return gson.toJson(json);
    }
}
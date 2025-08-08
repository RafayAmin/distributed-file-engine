// src/main/java/com/rafay/fileengine/auth/ClientManager.java
package com.rafay.fileengine.auth;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class ClientManager {
    private static final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, String> clientApiKeyMap = new HashMap<>();

    /**
     * Register a new client and generate a unique API key.
     * @param clientId The client's unique identifier.
     * @return The generated API key.
     */
    public String registerClient(String clientId) {
        String apiKey = generateApiKey();
        clientApiKeyMap.put(clientId, apiKey);
        return apiKey;
    }

    /**
     * Validate a client's API key.
     * @param clientId The client's identifier.
     * @param apiKey The provided API key.
     * @return true if the API key is valid, false otherwise.
     */
    public boolean validateApiKey(String clientId, String apiKey) {
        String expectedKey = clientApiKeyMap.get(clientId);
        return expectedKey != null && expectedKey.equals(apiKey);
    }

    /**
     * Generate a cryptographically secure API key.
     * @return A Base64-encoded 256-bit random key.
     */
    private String generateApiKey() {
        byte[] keyBytes = new byte[32]; // 256 bits
        secureRandom.nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }
}
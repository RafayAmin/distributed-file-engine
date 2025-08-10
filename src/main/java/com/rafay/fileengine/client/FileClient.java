// src/main/java/com/rafay/fileengine/client/FileClient.java
package com.rafay.fileengine.client;

import com.rafay.fileengine.proto.FileEngineProto;
import com.rafay.fileengine.proto.IndexServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Map;
import java.util.HashMap; // You need this import

public class FileClient {
    private final ManagedChannel channel;
    private final IndexServiceGrpc.IndexServiceBlockingStub blockingStub;
    private String apiKey; // Store the API key

    public FileClient(String host, int port) {
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.blockingStub = IndexServiceGrpc.newBlockingStub(channel);
    }

    // Method to register with the server and get an API key
    public void registerWithServer(String clientId) {
        FileEngineProto.RegisterRequest request = FileEngineProto.RegisterRequest.newBuilder()
                .setClientId(clientId)
                .build();

        FileEngineProto.RegisterReply reply = blockingStub.registerClient(request);

        if ("SUCCESS".equals(reply.getStatus())) {
            this.apiKey = reply.getApiKey();
            System.out.println("Client registered. API Key: " + this.apiKey);
        } else {
            throw new RuntimeException("Failed to register: " + reply.getMessage());
        }
    }

    // Corrected method: Takes a file path and a word frequency map
    public void sendIndexRequest(String filePath, Map<String, Integer> wordFreqs) {
        FileEngineProto.IndexRequest request = FileEngineProto.IndexRequest.newBuilder()
                .setClientId("C1")
                .setApiKey(this.apiKey) // Use the stored key
                .setFilePath(filePath) // Use 'filePath' parameter
                .putAllWordFrequencies(wordFreqs) // Use 'wordFreqs' parameter
                .build();

        FileEngineProto.IndexReply reply = blockingStub.computeIndex(request);
        System.out.println("Index Reply: " + reply.getStatus() + " - " + reply.getMessage());
    }

    // Corrected method: Takes search terms
    public void sendSearchRequest(String... terms) {
        FileEngineProto.SearchRequest request = FileEngineProto.SearchRequest.newBuilder()
                .setClientId("C1")
                .setApiKey(this.apiKey) // Use the stored key
                .addAllQueryTerms(java.util.Arrays.asList(terms))
                .build();

        FileEngineProto.SearchReply reply = blockingStub.computeSearch(request);
        System.out.println("Search Results: " + reply.getResultsList());
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws InterruptedException {
        FileClient client = new FileClient("localhost", 8080);
        try {
            // Register first to get the API key
            client.registerWithServer("C1");

            // Create a word frequency map for a document
            Map<String, Integer> wordFreqs = new HashMap<>();
            wordFreqs.put("hello", 2);
            wordFreqs.put("world", 1);
            wordFreqs.put("distributed", 3);
            wordFreqs.put("systems", 2);

            // Send the indexing request
            client.sendIndexRequest("/test/document1.txt", wordFreqs); //  Pass both arguments

            // Send a search request
            client.sendSearchRequest("hello", "distributed");
        } finally {
            client.shutdown();
        }
    }
}
package com.rafay.fileengine.client;

import com.rafay.fileengine.proto.FileEngineProto;
import com.rafay.fileengine.proto.IndexServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FileClient {
    private final ManagedChannel channel;
    private final IndexServiceGrpc.IndexServiceBlockingStub blockingStub;

    public FileClient(String host, int port) {
        // Create a channel to the server
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext() // No SSL/TLS for simplicity in this example
                .build();
        // Create a blocking stub (synchronous)
        this.blockingStub = IndexServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
    }

    public void sendIndexRequest(String clientId, String apiKey) { // Add apiKey parameter
        // Create a simple test document
        String docPath = "/test/document1.txt";

        // Simulate a word frequency map (e.g., from parsing a file)
        Map<String, Integer> wordFreqs = new HashMap<>();
        wordFreqs.put("hello", 2);
        wordFreqs.put("world", 1);
        wordFreqs.put("distributed", 3);
        wordFreqs.put("systems", 2);

        // Build the request
        FileEngineProto.IndexRequest request = FileEngineProto.IndexRequest.newBuilder()
                .setClientId(clientId)
                .setApiKey(apiKey) // ← Set the API key
                .setFilePath(docPath)
                .putAllWordFrequencies(wordFreqs)
                .build();

        // Send the request and get the reply
        FileEngineProto.IndexReply reply = blockingStub.computeIndex(request);
        System.out.println("Index Reply: " + reply.getStatus() + " - " + reply.getMessage());
    }

    public void sendSearchRequest(String clientId, String apiKey, String... terms) { // Add parameters
        // Build the request
        FileEngineProto.SearchRequest request = FileEngineProto.SearchRequest.newBuilder()
                .setClientId(clientId)
                .setApiKey(apiKey) // ← Set the API key
                .addAllQueryTerms(Arrays.asList(terms))
                .build();

        // Send the request and get the reply
        FileEngineProto.SearchReply reply = blockingStub.computeSearch(request);
        System.out.println("Search Results: " + reply.getResultsList());
    }

    public static void main(String[] args) throws InterruptedException {
        FileClient client = new FileClient("localhost", 8080);

        // Use the same clientId and apiKey that the server knows
        String clientId = "C1";
        String apiKey = "your-generated-api-key"; // This should match what the server has

        try {
            client.sendIndexRequest(clientId, apiKey);
            client.sendSearchRequest(clientId, apiKey, "distributed", "systems");
        } finally {
            client.shutdown();
        }
    }
}
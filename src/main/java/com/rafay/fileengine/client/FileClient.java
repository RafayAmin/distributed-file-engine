package com.rafay.fileengine.client;

import com.rafay.fileengine.proto.FileEngineProto;
import com.rafay.fileengine.proto.IndexServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

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

    public void sendIndexRequest() {
        // Create a simple test document
        String docPath = "/test/document1.txt";
        String clientId = "C1";

        // Simulate a word frequency map (e.g., from parsing a file)
        Map<String, Integer> wordFreqs = new HashMap<>();
        wordFreqs.put("hello", 2);
        wordFreqs.put("world", 1);
        wordFreqs.put("distributed", 3);
        wordFreqs.put("systems", 2);

        // Build the request
        FileEngineProto.IndexRequest request = FileEngineProto.IndexRequest.newBuilder()
                .setClientId(clientId)
                .setFilePath(docPath)
                .putAllWordFrequencies(wordFreqs)
                .build();

        // Send the request and get the reply
        FileEngineProto.IndexReply reply = blockingStub.computeIndex(request);
        System.out.println("Index Reply: " + reply.getStatus() + " - " + reply.getMessage());
    }

    public void sendSearchRequest() {
        // Build a search request for the word "distributed"
        FileEngineProto.SearchRequest request = FileEngineProto.SearchRequest.newBuilder()
                .addQueryTerms("distributed")
                .addQueryTerms("systems")
                .build();

        // Send the request and get the reply
        FileEngineProto.SearchReply reply = blockingStub.computeSearch(request);

        // Print the results
        System.out.println("Search Results for 'distributed systems':");
        for (FileEngineProto.SearchResult result : reply.getResultsList()) {
            System.out.println("  Found in: " + result.getDocumentPath() +
                    " (Frequency: " + result.getTotalFrequency() + ")");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        FileClient client = new FileClient("localhost", 8080);
        try {
            client.sendIndexRequest();  // First, index a document
            client.sendSearchRequest(); // Then, search for it
        } finally {
            client.shutdown();
        }
    }
}
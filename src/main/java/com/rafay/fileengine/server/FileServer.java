package com.rafay.fileengine.server;

import com.rafay.fileengine.auth.ClientManager;
import com.rafay.fileengine.proto.FileEngineProto;
import com.rafay.fileengine.proto.IndexServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class FileServer extends IndexServiceGrpc.IndexServiceImplBase {

    private final ClientManager clientManager = new ClientManager();
    private static final Logger logger = Logger.getLogger(FileServer.class.getName());

    // Global index: document path -> word -> frequency
    private final ConcurrentHashMap<String, Map<String, Integer>> globalIndex = new ConcurrentHashMap<>();

    private Server server;

    public void start(int port) throws IOException {
        server = ServerBuilder.forPort(port)
                .addService(this)
                .build()
                .start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down gRPC server since JVM is shutting down");
            FileServer.this.stop();
            logger.info("Server shut down");
        }));
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    @Override
    public void computeIndex(FileEngineProto.IndexRequest request, StreamObserver<FileEngineProto.IndexReply> responseObserver) {
        String clientId = request.getClientId();
        String providedApiKey = request.getApiKey(); // ← Get the API key from the request

        if (!clientManager.validateApiKey(clientId, providedApiKey)) {
            FileEngineProto.IndexReply reply = FileEngineProto.IndexReply.newBuilder()
                    .setStatus("ERROR")
                    .setMessage("Invalid API Key")
                    .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
            return;
        }

        // If valid, proceed with indexing
        String docPath = request.getFilePath();
        Map<String, Integer> wordFreqs = request.getWordFrequenciesMap();

        globalIndex.put(docPath, wordFreqs);
        logger.info("Indexed: " + docPath + " from client " + clientId);

        FileEngineProto.IndexReply reply = FileEngineProto.IndexReply.newBuilder()
                .setStatus("SUCCESS")
                .setMessage("Document indexed successfully")
                .build();

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    @Override
    public void computeSearch(FileEngineProto.SearchRequest request, StreamObserver<FileEngineProto.SearchReply> responseObserver) {
        String clientId = request.getClientId();
        String providedApiKey = request.getApiKey(); // Get the API key from the request

        // Validate the API key
        if (!clientManager.validateApiKey(clientId, providedApiKey)) {
            // Create a SearchReply with an error status
            FileEngineProto.SearchReply errorReply = FileEngineProto.SearchReply.newBuilder()
                    .setErrorMessage("Invalid API Key") // Use a dedicated field for errors
                    .build();
            responseObserver.onNext(errorReply);
            responseObserver.onCompleted();
            return;
        }

        // If valid, proceed with the search
        FileEngineProto.SearchReply.Builder replyBuilder = FileEngineProto.SearchReply.newBuilder();

        for (Map.Entry<String, Map<String, Integer>> docEntry : globalIndex.entrySet()) {
            int totalFreq = 0;
            for (String queryTerm : request.getQueryTermsList()) {
                totalFreq += docEntry.getValue().getOrDefault(queryTerm.toLowerCase(), 0);
            }
            if (totalFreq > 0) {
                FileEngineProto.SearchResult result = FileEngineProto.SearchResult.newBuilder()
                        .setDocumentPath(docEntry.getKey())
                        .setTotalFrequency(totalFreq)
                        .build();
                replyBuilder.addResults(result);
            }
        }

        responseObserver.onNext(replyBuilder.build());
        responseObserver.onCompleted();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        FileServer server = new FileServer();

        // Register a test client
        String clientId = "C1";
        String apiKey = server.clientManager.registerClient(clientId);
        System.out.println("Registered Client ID: " + clientId);
        System.out.println("API Key: " + apiKey); // In real use, this would be sent securely to the client

        int port = 8080;
        server.start(port);
        server.blockUntilShutdown();
    }
}
package com.rafay.fileengine.server;

import com.rafay.fileengine.auth.ClientManager;
import com.rafay.fileengine.proto.FileEngineProto;
import com.rafay.fileengine.proto.IndexServiceGrpc;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class FileServer extends IndexServiceGrpc.IndexServiceImplBase {

    private final ClientManager clientManager = new ClientManager();
    private static final Logger logger = Logger.getLogger(FileServer.class.getName());
    private final ConcurrentHashMap<String, Map<String, Integer>> globalIndex = new ConcurrentHashMap<>();
    private Server server;

    public void start(int port) throws IOException {
        // Add the Interceptor here so we can capture the attacker's IP address
        server = ServerBuilder.forPort(port)
                .addService(ServerInterceptors.intercept(this, new ClientIPInterceptor()))
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
        String providedApiKey = request.getApiKey();
        // Get the attacker's IP address
        String clientIp = ClientIPInterceptor.CLIENT_IP.get();

        if (!clientManager.validateApiKey(clientId, providedApiKey)) {
            // LOG FOR BRUTE-FORCE DETECTION (Rule 100100 & 100101)
            System.out.println("authentication failed: invalid api key for client " + clientId + " from IP " + clientIp);

            FileEngineProto.IndexReply reply = FileEngineProto.IndexReply.newBuilder()
                    .setStatus("ERROR")
                    .setMessage("Invalid API Key")
                    .build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
            return;
        }

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
        String providedApiKey = request.getApiKey();
        // Get the attacker's IP address
        String clientIp = ClientIPInterceptor.CLIENT_IP.get();

        // LOG FOR RESOURCE EXHAUSTION DETECTION (Rule 100103)
        if (request.getQueryTermsCount() > 1000) {
            System.out.println("resource exhaustion: massive query detected from client " + clientId + " from IP " + clientIp);
        }

        if (!clientManager.validateApiKey(clientId, providedApiKey)) {
            // LOG FOR BRUTE-FORCE DETECTION (Rule 100100 & 100101)
            System.out.println("authentication failed: invalid api key for client " + clientId + " from IP " + clientIp);

            FileEngineProto.SearchReply errorReply = FileEngineProto.SearchReply.newBuilder()
                    .setErrorMessage("Invalid API Key")
                    .build();
            responseObserver.onNext(errorReply);
            responseObserver.onCompleted();
            return;
        }

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

    @Override
    public void registerClient(FileEngineProto.RegisterRequest request, StreamObserver<FileEngineProto.RegisterReply> responseObserver) {
        String clientId = request.getClientId();
        // Get the attacker's IP address
        String clientIp = ClientIPInterceptor.CLIENT_IP.get();
        
        // LOG FOR API ABUSE DETECTION (Rule 100102)
        System.out.println("rate limit exceeded: registration spam detected for " + clientId + " from IP " + clientIp);

        String apiKey = clientManager.registerClient(clientId);
        FileEngineProto.RegisterReply reply = FileEngineProto.RegisterReply.newBuilder()
                .setApiKey(apiKey)
                .setStatus("SUCCESS")
                .setMessage("Client registered successfully")
                .build();

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        FileServer server = new FileServer();
        int port = Integer.parseInt(System.getenv().getOrDefault("SERVER_PORT", "8080").trim());
        String clientId = System.getenv().getOrDefault("TEST_CLIENT_ID", "C1");
        String apiKey = server.clientManager.registerClient(clientId);
        System.out.println("Registered Client ID: " + clientId);
        System.out.println("API Key: " + apiKey);
        System.out.println("Server listening on port: " + port);

        server.start(port);
        server.blockUntilShutdown();
    }
}

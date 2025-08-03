// src/main/java/com/rafay/fileengine/server/ZeroMQFileServer.java
package com.rafay.fileengine.server;

import com.rafay.fileengine.common.MessageUtils;
import org.zeromq.ZMQ;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@SuppressWarnings("deprecation") // Suppresses warnings for ZMQ class usage
public class ZeroMQFileServer {
    private static final Logger logger = Logger.getLogger(ZeroMQFileServer.class.getName());
    private final ZMQ.Context context = ZMQ.context(1); // Use ZMQ.context()
    private ZMQ.Socket dealerSocket;
    private Thread serverThread;

    // Global index: document path -> word -> frequency
    private final ConcurrentHashMap<String, Map<String, Integer>> globalIndex = new ConcurrentHashMap<>();

    public void start(int port) {
        serverThread = new Thread(() -> {
            dealerSocket = context.socket(ZMQ.DEALER); // Use context.socket()
            dealerSocket.bind("tcp://*:" + port);
            logger.info("ZeroMQ Server started on port " + port);

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Receive message from a client (includes client ID)
                    byte[] identity = dealerSocket.recv(0); // Client identity
                    dealerSocket.recv(0); // Empty delimiter (discarded)
                    String message = dealerSocket.recvStr(0); // Actual message

                    logger.info("Received message from client");

                    String messageType = MessageUtils.getMessageType(message);

                    if ("INDEX".equals(messageType)) {
                        handleIndexRequest(identity, message);
                    } else if ("SEARCH".equals(messageType)) {
                        handleSearchRequest(identity, message);
                    }
                } catch (Exception e) {
                    if (!Thread.currentThread().isInterrupted()) {
                        logger.severe("Error in server loop: " + e.getMessage());
                    }
                }
            }

            dealerSocket.close();
            context.term(); // Use context.term() to terminate
        });
        serverThread.start();
    }

    private void handleIndexRequest(byte[] identity, String message) {
        String clientId = MessageUtils.getClientId(message);
        String filePath = MessageUtils.getFilePath(message);
        Map<String, Integer> wordFreqs = MessageUtils.getWordFrequencies(message);

        globalIndex.put(filePath, wordFreqs);
        logger.info("Indexed: " + filePath + " from client " + clientId);

        String reply = MessageUtils.createIndexReply("SUCCESS", "Document indexed successfully");

        dealerSocket.send(identity, ZMQ.SNDMORE);
        dealerSocket.send(new byte[0], ZMQ.SNDMORE); // Send empty delimiter
        dealerSocket.send(reply, 0);
    }

    private void handleSearchRequest(byte[] identity, String message) {
        String[] queryTerms = MessageUtils.getQueryTerms(message);

        ConcurrentHashMap<String, Integer> results = new ConcurrentHashMap<>();
        for (Map.Entry<String, Map<String, Integer>> docEntry : globalIndex.entrySet()) {
            int totalFreq = 0;
            for (String term : queryTerms) {
                totalFreq += docEntry.getValue().getOrDefault(term.toLowerCase(), 0);
            }
            if (totalFreq > 0) {
                results.put(docEntry.getKey(), totalFreq);
            }
        }

        String reply = MessageUtils.createSearchReply(results);

        dealerSocket.send(identity, ZMQ.SNDMORE);
        dealerSocket.send(new byte[0], ZMQ.SNDMORE); // Send empty delimiter
        dealerSocket.send(reply, 0);
    }

    public void stop() {
        if (serverThread != null) {
            serverThread.interrupt();
            try {
                serverThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        context.term(); // Terminate the context
    }

    public static void main(String[] args) {
        ZeroMQFileServer server = new ZeroMQFileServer();
        int port = 9090;
        server.start(port);
        System.out.println("ZeroMQ File Server started on port " + port);

        // Keep server running
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            server.stop();
        }
    }
}
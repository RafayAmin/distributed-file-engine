// src/main/java/com/rafay/fileengine/client/ZeroMQFileClient.java
package com.rafay.fileengine.client;

import com.rafay.fileengine.common.MessageUtils;
import org.zeromq.ZMQ;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("deprecation") // Suppresses warnings for ZMQ class usage
public class ZeroMQFileClient {
    private final ZMQ.Context context = ZMQ.context(1);
    private ZMQ.Socket dealerSocket;
    private final String clientId;

    public ZeroMQFileClient(String clientId) {
        this.clientId = clientId;
        this.dealerSocket = context.socket(ZMQ.DEALER);
        // Set the client identity so the server can route replies back
        dealerSocket.setIdentity(clientId.getBytes());
    }

    /**
     * Connect to the ZeroMQ server.
     * @param serverAddress The server's IP or hostname (e.g., "localhost").
     * @param port The port the server is listening on (e.g., 9090).
     */
    public void connect(String serverAddress, int port) {
        dealerSocket.connect("tcp://" + serverAddress + ":" + port);
        System.out.println("Connected to server at " + serverAddress + ":" + port);
    }

    /**
     * Send an indexing request to the server.
     * @param filePath The path of the document being indexed.
     * @param wordFreqs A map of word frequencies from the document.
     */
    public void sendIndexRequest(String filePath, Map<String, Integer> wordFreqs) {
        String request = MessageUtils.createIndexRequest(clientId, filePath, wordFreqs);
        dealerSocket.send(request, 0);

        // Receive the reply from the server
        String reply = dealerSocket.recvStr(0);
        System.out.println("Index Reply: " + reply);
    }

    /**
     * Send a search request to the server.
     * @param terms The search terms.
     */
    public void sendSearchRequest(String... terms) {
        String request = MessageUtils.createSearchRequest(terms);
        dealerSocket.send(request, 0);

        // Receive the reply from the server
        String reply = dealerSocket.recvStr(0);
        System.out.println("Search Reply: " + reply);
    }

    /**
     * Close the client connection.
     */
    public void close() {
        dealerSocket.close();
        context.term(); // Terminate the ZMQ context
    }

    /**
     * Main method for testing the client.
     */
    public static void main(String[] args) {
        ZeroMQFileClient client = new ZeroMQFileClient("C1");
        try {
            client.connect("localhost", 9090);

            // Simulate a document with word frequencies
            Map<String, Integer> wordFreqs = new HashMap<>();
            wordFreqs.put("hello", 2);
            wordFreqs.put("world", 1);
            wordFreqs.put("zeromq", 3);

            // Send an indexing request
            client.sendIndexRequest("/test/zeromq_doc.txt", wordFreqs);

            // Send a search request
            client.sendSearchRequest("hello", "zeromq");

        } finally {
            client.close();
        }
    }
}
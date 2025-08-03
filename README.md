# Distributed File Retrieval Engine

A scalable, distributed system for indexing and searching text files, built with **gRPC** and **Protocol Buffers**. This project demonstrates core concepts in distributed systems, including client-server architecture, remote procedure calls (RPC), and client-side computation for improved server scalability.

## Features

- **gRPC Communication**: Uses Protocol Buffers for efficient, typed communication between client and server.
- **Client-Side Computation**: Word counting is performed on the client to reduce server load and improve scalability.
- **Global Index**: The server maintains a central index of word frequencies across all documents.
- **Search Functionality**: Clients can search for keywords and retrieve a ranked list of matching documents.
- **Maven Build**: Fully integrated with Maven for dependency management and compilation.

## Project Structure
distributed-file-engine/
├── pom.xml # Maven configuration file
├── src/
│ ├── main/
│ │ ├── java/
│ │ │ └── com/rafay/fileengine/
│ │ │ ├── server/
│ │ │ │ └── FileServer.java # gRPC server implementation
│ │ │ └── client/
│ │ │ └── FileClient.java # gRPC client implementation
│ │ └── proto/
│ │ └── file_engine.proto # gRPC service and message definitions
└── README.md

## How to Build

1.  **Prerequisites**:
    - Java 21 (JDK)
    - Apache Maven 3.9+
    - Git

2.  **Clone the Repository**:
    ```bash
    git clone https://github.com/RafayAmin/distributed-file-engine.git
    cd distributed-file-engine
    ```

3.  **Build the Project**:
    ```bash
    mvn clean compile
    ```
    This command will:
    - Download all dependencies (gRPC, Protobuf, etc.).
    - Compile the `.proto` file into Java code.
    - Compile the entire Java project.

## How to Run

### 1. Start the Server

In one terminal, start the server on port `8080`:

```bash
mvn exec:java -Dexec.mainClass="com.rafay.fileengine.server.FileServer"
```
Alternatively, you can run FileServer.java directly from your IDE (e.g., IntelliJ IDEA).

You should see:
```bash
INFO: Server started, listening on 8080
```

In a second terminal, run the client:
```bash
mvn exec:java -Dexec.mainClass="com.rafay.fileengine.client.FileClient"
```
Alternatively, run FileClient.java from your IDE.

The client will:

Connect to the server.
Send an indexing request for a test document (/test/document1.txt).
Send a search request for the words "distributed" and "systems".
Print the results.
You should see output like:
```bash
Index Reply: SUCCESS - Document indexed successfully
Search Results for 'distributed systems':
Found in: /test/document1.txt (Frequency: 5)
```

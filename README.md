# Java NIO Multi-Client Chat Server

A scalable, non-blocking chat server built with Java NIO, supporting multiple concurrent clients using a selector-driven architecture and thread pools.

## Motivation
Applying theoretical knowledge acquired through my internship and applying it programmatically using Java. 

My first design had four threads per client. Messages, that I traditionally called packets, were received and transceived using two clients assigned to the home channel. The server had two more threads that were created when a client connected to it, and assigned these threads to a threadpool to run indefinently. This version had scalability issues, was not using the idea of tasks and threadpools correctly, and was not as informative as using NIO to learn about socket channels.

My second version explores a non-blocking approach using Java NIO to iterate through registered SelectionKeys that are bounded to client and server channels. This approach allows me to:
- Handle many clients with a single selector thread
- Avoid thread-per-connection overhead
- Maintain responsiveness under load
- Become closer to understan

## Architecture (Needs to be updated)
- **ServerSocketChannel**: Accepts incoming connections
- **Selector**: Monitors channels for readiness (OP_ACCEPT, OP_READ, OP_WRITE)
- **SocketChannel**: Handles client communication
- **Thread Pool**: Processes messages without blocking the selector loop

### Flow (Needs to be updated)
1. Selector waits for events
2. Accept new clients (OP_ACCEPT)
3. Read incoming messages (OP_READ)
4. Dispatch processing to thread pool
5. Broadcast messages to other clients

### Diagram (TBD)

## Key Concepts (Needs to be updated)
- Non-blocking I/O with Java NIO
- Selector pattern for multiplexing channels
- Thread pool offloading for scalability
- Concurrent message handling
- Backpressure awareness (write readiness)

## Running the Server 

```bash
javac *.java
java ClientInit.java
java ServerInit.java
```


## Future Improvements

- Message persistence
- Authentication
- Private messaging
- Horizontal scaling
- Protocol abstraction (e.g., WebSocket)


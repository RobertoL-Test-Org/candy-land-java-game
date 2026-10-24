package com.example.candyland.network.client;

import com.example.candyland.network.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;

/**
 * Client for connecting to a Candy Land multiplayer server.
 */
public class GameClient {
    private static final Logger logger = LoggerFactory.getLogger(GameClient.class);
    
    private final String serverHost;
    private final int serverPort;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Thread listenerThread;
    private volatile boolean connected;
    private GameMessageListener messageListener;
    
    /**
     * Interface for receiving game messages.
     */
    public interface GameMessageListener {
        void onMessageReceived(GameMessage message);
        void onConnectionLost();
    }
    
    public GameClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.connected = false;
    }
    
    /**
     * Sets the message listener for receiving game updates.
     */
    public void setMessageListener(GameMessageListener listener) {
        this.messageListener = listener;
    }
    
    /**
     * Connects to the game server.
     */
    public void connect() throws IOException {
        logger.info("Connecting to server at {}:{}", serverHost, serverPort);
        socket = new Socket(serverHost, serverPort);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        connected = true;
        
        // Start listener thread
        listenerThread = new Thread(this::listenForMessages);
        listenerThread.setDaemon(true);
        listenerThread.start();
        
        logger.info("Connected to server");
    }
    
    /**
     * Joins a game room.
     */
    public void joinGame(String playerName, String gameRoomId, String authToken) throws IOException {
        sendMessage(new JoinGameMessage(playerName, gameRoomId, authToken));
    }
    
    /**
     * Requests to draw a card.
     */
    public void drawCard(String playerName) throws IOException {
        sendMessage(new DrawCardMessage(playerName));
    }
    
    /**
     * Sends a message to the server.
     */
    private synchronized void sendMessage(GameMessage message) throws IOException {
        if (!connected) {
            throw new IOException("Not connected to server");
        }
        out.writeObject(message);
        out.flush();
    }
    
    /**
     * Listens for messages from the server.
     */
    private void listenForMessages() {
        try {
            while (connected && !Thread.currentThread().isInterrupted()) {
                GameMessage message = (GameMessage) in.readObject();
                if (messageListener != null) {
                    messageListener.onMessageReceived(message);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if (connected) {
                logger.error("Connection lost: {}", e.getMessage());
                disconnect();
                if (messageListener != null) {
                    messageListener.onConnectionLost();
                }
            }
        }
    }
    
    /**
     * Disconnects from the server.
     */
    public void disconnect() {
        connected = false;
        try {
            if (listenerThread != null && listenerThread.isAlive()) {
                listenerThread.interrupt();
            }
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            logger.info("Disconnected from server");
        } catch (IOException e) {
            logger.error("Error during disconnect", e);
        }
    }
    
    /**
     * Checks if connected to the server.
     */
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
}

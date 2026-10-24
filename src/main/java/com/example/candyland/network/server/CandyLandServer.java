package com.example.candyland.network.server;

import com.example.candyland.*;
import com.example.candyland.network.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/**
 * Server for hosting multiplayer Candy Land games.
 * Manages game rooms and player connections.
 */
public class CandyLandServer {
    private static final Logger logger = LoggerFactory.getLogger(CandyLandServer.class);
    private static final int DEFAULT_PORT = 8888;
    private static final int MAX_PLAYERS_PER_GAME = 3;
    private static final String SECRET_ENV = "CANDYLAND_SERVER_SECRET";
    
    private final int port;
    private final byte[] sharedSecret;
    private final Map<String, GameRoom> gameRooms;
    private final ExecutorService executorService;
    private ServerSocket serverSocket;
    private volatile boolean running;
    
    public CandyLandServer(int port, String sharedSecret) {
        if (sharedSecret == null || sharedSecret.isBlank()) {
            throw new IllegalArgumentException("A non-blank shared secret is required");
        }
        this.port = port;
        this.sharedSecret = sharedSecret.getBytes(StandardCharsets.UTF_8);
        this.gameRooms = new ConcurrentHashMap<>();
        this.executorService = Executors.newCachedThreadPool();
        this.running = false;
    }
    
    /**
     * Compares a client-supplied token against the expected secret in constant time.
     */
    static boolean isValidToken(byte[] expected, String supplied) {
        if (supplied == null) {
            return false;
        }
        return MessageDigest.isEqual(expected, supplied.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Starts the server and begins accepting client connections.
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        logger.info("Candy Land Server started on port {}", port);
        
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                logger.info("New client connected from {}", clientSocket.getInetAddress());
                executorService.execute(new ClientHandler(clientSocket));
            } catch (IOException e) {
                if (running) {
                    logger.error("Error accepting client connection", e);
                }
            }
        }
    }
    
    /**
     * Stops the server and closes all connections.
     */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            executorService.shutdown();
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            gameRooms.values().forEach(GameRoom::closeAllConnections);
            logger.info("Server stopped");
        } catch (IOException | InterruptedException e) {
            logger.error("Error stopping server", e);
        }
    }
    
    /**
     * Gets or creates a game room.
     */
    private synchronized GameRoom getOrCreateGameRoom(String roomId) {
        return gameRooms.computeIfAbsent(roomId, id -> {
            logger.info("Creating new game room: {}", id);
            return new GameRoom(id);
        });
    }
    
    /**
     * Handles communication with a single client.
     */
    private class ClientHandler implements Runnable {
        private final Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private GameRoom gameRoom;
        private String playerName;
        private boolean authenticated = false;
        
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());
                
                while (running && !socket.isClosed()) {
                    GameMessage message = (GameMessage) in.readObject();
                    handleMessage(message);
                }
            } catch (IOException | ClassNotFoundException e) {
                logger.warn("Client disconnected: {}", e.getMessage());
            } finally {
                cleanup();
            }
        }
        
        private void handleMessage(GameMessage message) throws IOException {
            switch (message.getType()) {
                case JOIN_GAME:
                    if (authenticated) {
                        sendMessage(new ErrorMessage("Already joined", ErrorMessage.ErrorCode.INVALID_PLAYER));
                        return;
                    }
                    handleJoinGame((JoinGameMessage) message);
                    break;
                case DRAW_CARD:
                    if (!authenticated) {
                        sendMessage(new ErrorMessage("Not authenticated", ErrorMessage.ErrorCode.INVALID_PLAYER));
                        socket.close();
                        return;
                    }
                    handleDrawCard((DrawCardMessage) message);
                    break;
                case DISCONNECT:
                    cleanup();
                    break;
                default:
                    logger.warn("Unknown message type: {}", message.getType());
            }
        }
        
        private void handleJoinGame(JoinGameMessage message) throws IOException {
            if (!isValidToken(sharedSecret, message.getAuthToken())) {
                sendMessage(new ErrorMessage("Authentication failed", ErrorMessage.ErrorCode.INVALID_PLAYER));
                socket.close();
                return;
            }
            
            String requestedName = message.getPlayerName();
            String roomId = message.getGameRoomId();
            if (requestedName == null || requestedName.isBlank() || roomId == null || roomId.isBlank()) {
                sendMessage(new ErrorMessage("Invalid player name or room", ErrorMessage.ErrorCode.INVALID_PLAYER));
                return;
            }
            
            GameRoom room = getOrCreateGameRoom(roomId);
            
            if (room.getPlayerCount() >= MAX_PLAYERS_PER_GAME) {
                sendMessage(new ErrorMessage("Game is full", ErrorMessage.ErrorCode.GAME_FULL));
                socket.close();
                return;
            }
            
            if (room.addPlayer(requestedName, this)) {
                this.playerName = requestedName;
                this.gameRoom = room;
                this.authenticated = true;
                logger.info("Player {} joined room {}", playerName, roomId);
            }
        }
        
        private void handleDrawCard(DrawCardMessage message) throws IOException {
            if (gameRoom == null) {
                sendMessage(new ErrorMessage("Not in a game room", ErrorMessage.ErrorCode.INVALID_PLAYER));
                return;
            }
            
            gameRoom.processDrawCard(this.playerName);
        }
        
        public void sendMessage(GameMessage message) throws IOException {
            synchronized (out) {
                out.writeObject(message);
                out.flush();
            }
        }
        
        private void cleanup() {
            try {
                if (gameRoom != null && playerName != null) {
                    gameRoom.removePlayer(playerName);
                }
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                logger.error("Error during cleanup", e);
            }
        }
    }
    
    /**
     * Represents a game room with multiple players.
     */
    private static class GameRoom {
        private final String roomId;
        private final CandyLandGame game;
        private final Map<String, ClientHandler> players;
        private final List<String> playerOrder;
        private int currentPlayerIndex;
        private boolean gameStarted;
        
        public GameRoom(String roomId) {
            this.roomId = roomId;
            this.game = new CandyLandGame();
            this.players = new ConcurrentHashMap<>();
            this.playerOrder = new ArrayList<>();
            this.currentPlayerIndex = 0;
            this.gameStarted = false;
        }
        
        public synchronized boolean addPlayer(String playerName, ClientHandler handler) throws IOException {
            if (players.containsKey(playerName)) {
                handler.sendMessage(new ErrorMessage("Player name already taken", 
                    ErrorMessage.ErrorCode.INVALID_PLAYER));
                return false;
            }
            
            players.put(playerName, handler);
            playerOrder.add(playerName);
            game.addPlayer(playerName);
            
            // Notify all players
            PlayerJoinedMessage joinMsg = new PlayerJoinedMessage(
                playerName, playerOrder.size() - 1, playerOrder.size());
            broadcastMessage(joinMsg);
            
            // Send current game state to new player
            handler.sendMessage(createGameStateMessage());
            
            // Start game if we have at least 2 players
            if (!gameStarted && playerOrder.size() >= 2) {
                gameStarted = true;
                logger.info("Game started in room {} with {} players", roomId, playerOrder.size());
                broadcastMessage(createGameStateMessage());
            }
            return true;
        }
        
        public synchronized void removePlayer(String playerName) {
            players.remove(playerName);
            playerOrder.remove(playerName);
            
            if (players.isEmpty()) {
                logger.info("Room {} is empty, cleaning up", roomId);
            }
        }
        
        public synchronized void processDrawCard(String playerName) throws IOException {
            if (!gameStarted) {
                players.get(playerName).sendMessage(
                    new ErrorMessage("Game hasn't started yet", ErrorMessage.ErrorCode.INVALID_TURN));
                return;
            }
            
            if (!playerName.equals(getCurrentPlayerName())) {
                players.get(playerName).sendMessage(
                    new ErrorMessage("Not your turn", ErrorMessage.ErrorCode.INVALID_TURN));
                return;
            }
            
            // Get current player
            Player player = game.getPlayers().get(currentPlayerIndex);
            int oldPosition = player.getPosition();
            
            // Draw card and move player
            Card card = game.getDeck().drawCard();
            int newPosition = game.getBoard().calculateNewPosition(oldPosition, card);
            player.setPosition(newPosition);
            
            String moveDescription = String.format("%s drew %s and moved from %d to %d",
                playerName, card, oldPosition, newPosition);
            logger.info(moveDescription);
            
            // Send card drawn message
            CardDrawnMessage cardMsg = new CardDrawnMessage(
                playerName, card, oldPosition, newPosition, moveDescription);
            broadcastMessage(cardMsg);
            
            // Check for winner
            if (newPosition >= game.getBoard().getWinningPosition()) {
                GameOverMessage gameOverMsg = new GameOverMessage(playerName, newPosition);
                broadcastMessage(gameOverMsg);
                gameStarted = false;
            } else {
                // Move to next player
                currentPlayerIndex = (currentPlayerIndex + 1) % playerOrder.size();
                broadcastMessage(createGameStateMessage());
            }
        }
        
        private GameStateMessage createGameStateMessage() {
            List<GameStateMessage.PlayerState> playerStates = new ArrayList<>();
            for (String name : playerOrder) {
                Player player = game.getPlayers().stream()
                    .filter(p -> p.getName().equals(name))
                    .findFirst()
                    .orElse(null);
                
                if (player != null) {
                    playerStates.add(new GameStateMessage.PlayerState(
                        player, players.containsKey(name)));
                }
            }
            
            String winner = null;
            boolean gameOver = false;
            if (!gameStarted && !playerOrder.isEmpty()) {
                for (Player p : game.getPlayers()) {
                    if (p.getPosition() >= game.getBoard().getWinningPosition()) {
                        winner = p.getName();
                        gameOver = true;
                        break;
                    }
                }
            }
            
            return new GameStateMessage(playerStates, currentPlayerIndex, gameOver, winner);
        }
        
        private void broadcastMessage(GameMessage message) {
            for (ClientHandler handler : players.values()) {
                try {
                    handler.sendMessage(message);
                } catch (IOException e) {
                    logger.error("Error sending message to player", e);
                }
            }
        }
        
        private String getCurrentPlayerName() {
            return playerOrder.isEmpty() ? null : playerOrder.get(currentPlayerIndex);
        }
        
        public int getPlayerCount() {
            return players.size();
        }
        
        public void closeAllConnections() {
            for (ClientHandler handler : players.values()) {
                handler.cleanup();
            }
        }
    }
    
    /**
     * Main method to start the server.
     */
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                logger.error("Invalid port number, using default: {}", DEFAULT_PORT);
            }
        }
        
        String secret = System.getenv(SECRET_ENV);
        if (secret == null || secret.isBlank()) {
            logger.error("{} must be set to a non-blank shared secret", SECRET_ENV);
            System.exit(1);
            return;
        }
        
        CandyLandServer server = new CandyLandServer(port, secret);
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        
        try {
            server.start();
        } catch (IOException e) {
            logger.error("Failed to start server", e);
            System.exit(1);
        }
    }
}

package com.example.candyland.gui;

import com.example.candyland.*;
import com.example.candyland.network.*;
import com.example.candyland.network.client.GameClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.List;

/**
 * Main GUI window for the Candy Land game.
 */
public class CandyLandGUI extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(CandyLandGUI.class);
    
    private transient CandyLandGame game;
    private transient GameBoardPanel boardPanel;
    private transient GameControlPanel controlPanel;
    private transient GameClient networkClient;
    private boolean isMultiplayer;
    private String currentPlayerName;
    
    /**
     * Creates and displays the Candy Land GUI.
     */
    public CandyLandGUI() {
        // Show game mode selection
        showGameModeSelection();
    }
    
    /**
     * Shows dialog to select game mode (local or online multiplayer).
     */
    private void showGameModeSelection() {
        String[] options = {"Local Game", "Join Online Game", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this,
            "Select Game Mode:",
            "Candy Land - Game Mode",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]);
        
        if (choice == 0) {
            // Local game
            isMultiplayer = false;
            initializeLocalGame();
        } else if (choice == 1) {
            // Online multiplayer
            isMultiplayer = true;
            initializeMultiplayerGame();
        } else {
            // Cancel
            System.exit(0);
        }
        
        if (game != null) {
            initializeGUI();
            setVisible(true);
        } else {
            System.exit(0);
        }
    }
    
    /**
     * Initializes a local game.
     */
    private void initializeLocalGame() {
        initializeGame();
    }
    
    /**
     * Initializes an online multiplayer game.
     */
    private void initializeMultiplayerGame() {
        MultiplayerConnectionDialog connectionDialog = new MultiplayerConnectionDialog(this);
        connectionDialog.setVisible(true);
        
        if (!connectionDialog.isConfirmed()) {
            return;
        }
        
        String serverHost = connectionDialog.getServerHost();
        int serverPort = connectionDialog.getServerPort();
        currentPlayerName = connectionDialog.getPlayerName();
        String roomId = connectionDialog.getRoomId();
        
        try {
            // Create network client
            networkClient = new GameClient(serverHost, serverPort);
            networkClient.setMessageListener(new NetworkMessageHandler());
            networkClient.connect();
            
            // Initialize empty game that will be updated from server
            game = new CandyLandGame();
            
            // Join the game
            networkClient.joinGame(currentPlayerName, roomId, System.getenv("CANDYLAND_SERVER_SECRET"));
            
            JOptionPane.showMessageDialog(this,
                "Connected to server! Waiting for other players...",
                "Connected",
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (IOException e) {
            logger.error("Failed to connect to server", e);
            JOptionPane.showMessageDialog(this,
                "Failed to connect to server: " + e.getMessage(),
                "Connection Error",
                JOptionPane.ERROR_MESSAGE);
            isMultiplayer = false;
        }
    }
    
    /**
     * Initializes the game with player setup.
     */
    private void initializeGame() {
        PlayerSetupDialog setupDialog = new PlayerSetupDialog(this);
        setupDialog.setVisible(true);
        
        if (setupDialog.isSetupComplete()) {
            List<String> playerNames = setupDialog.getPlayerNames();
            game = new CandyLandGame();
            
            // Add players to the game
            for (String name : playerNames) {
                game.addPlayer(name);
            }
        }
    }
    
    /**
     * Initializes the GUI components.
     */
    private void initializeGUI() {
        setTitle("Candy Land - The Board Game");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Create board panel
        boardPanel = new GameBoardPanel(game.getBoard(), game.getPlayers());
        JScrollPane boardScrollPane = new JScrollPane(boardPanel);
        boardScrollPane.setPreferredSize(new Dimension(800, 600));
        add(boardScrollPane, BorderLayout.CENTER);
        
        // Create control panel
        controlPanel = new GameControlPanel(game, boardPanel, this);
        add(controlPanel, BorderLayout.EAST);
        
        // Create menu bar
        createMenuBar();
        
        // Set window properties
        pack();
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(1000, 700));
    }
    
    /**
     * Creates the menu bar.
     */
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // Game menu
        JMenu gameMenu = new JMenu("Game");
        
        JMenuItem newLocalGameItem = new JMenuItem("New Local Game");
        newLocalGameItem.addActionListener(e -> startNewLocalGame());
        gameMenu.add(newLocalGameItem);
        
        JMenuItem newOnlineGameItem = new JMenuItem("Join Online Game");
        newOnlineGameItem.addActionListener(e -> startNewOnlineGame());
        gameMenu.add(newOnlineGameItem);
        
        gameMenu.addSeparator();
        
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> {
            if (networkClient != null && networkClient.isConnected()) {
                networkClient.disconnect();
            }
            System.exit(0);
        });
        gameMenu.add(exitItem);
        
        // Help menu
        JMenu helpMenu = new JMenu("Help");
        
        JMenuItem rulesItem = new JMenuItem("Game Rules");
        rulesItem.addActionListener(e -> showGameRules());
        helpMenu.add(rulesItem);
        
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAbout());
        helpMenu.add(aboutItem);
        
        menuBar.add(gameMenu);
        menuBar.add(helpMenu);
        
        setJMenuBar(menuBar);
    }
    
    /**
     * Starts a new local game.
     */
    private void startNewLocalGame() {
        int option = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to start a new game?", 
            "New Game", 
            JOptionPane.YES_NO_OPTION);
            
        if (option == JOptionPane.YES_OPTION) {
            if (networkClient != null && networkClient.isConnected()) {
                networkClient.disconnect();
            }
            dispose();
            SwingUtilities.invokeLater(CandyLandGUI::new);
        }
    }
    
    /**
     * Starts a new online multiplayer game.
     */
    private void startNewOnlineGame() {
        int option = JOptionPane.showConfirmDialog(this, 
            "This will disconnect from the current game. Continue?", 
            "Join Online Game", 
            JOptionPane.YES_NO_OPTION);
            
        if (option == JOptionPane.YES_OPTION) {
            if (networkClient != null && networkClient.isConnected()) {
                networkClient.disconnect();
            }
            isMultiplayer = true;
            initializeMultiplayerGame();
            if (game != null) {
                dispose();
                initializeGUI();
                setVisible(true);
            }
        }
    }
    
    /**
     * Starts a new game (legacy method - now prompts for mode).
     */
    private void startNewGame() {
        startNewLocalGame();
    }
    
    /**
     * Shows the game rules dialog.
     */
    private void showGameRules() {
        String rules = """
            Candy Land Game Rules:
            
            1. Each player takes turns drawing cards
            2. Move to the next space of the color shown on your card
            3. Double color cards move to the second occurrence of that color
            4. Special character cards move directly to that character's location
            5. Players never move backward with character cards
            6. The first player to reach or pass the finish line wins!
            
            Special Characters:
            - Plumpy (Position 8)
            - Mr. Mint (Position 17)
            - Jolly (Position 33)
            - Lord Licorice (Position 47)
            - Gramma Nutt (Position 75)
            - Princess Lolly (Position 95)
            - Queen Frostine (Position 104)
            - King Kandy (Position 118)
            """;
            
        JOptionPane.showMessageDialog(this, rules, "Game Rules", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Shows the about dialog.
     */
    private void showAbout() {
        String about = """
            Candy Land - Java Implementation
            
            A classic board game for 1-3 players.
            
            Developed using Java Swing for educational purposes.
            
            Version 1.0
            """;
            
        JOptionPane.showMessageDialog(this, about, "About Candy Land", JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * Main method to start the GUI application.
     * 
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        // Start the application
        SwingUtilities.invokeLater(CandyLandGUI::new);
    }
    
    /**
     * Handles network messages from the multiplayer server.
     */
    private class NetworkMessageHandler implements GameClient.GameMessageListener {
        @Override
        public void onMessageReceived(GameMessage message) {
            SwingUtilities.invokeLater(() -> handleNetworkMessage(message));
        }
        
        @Override
        public void onConnectionLost() {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(CandyLandGUI.this,
                    "Connection to server lost!",
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
                if (networkClient != null) {
                    networkClient.disconnect();
                }
            });
        }
    }
    
    /**
     * Handles incoming network messages.
     */
    private void handleNetworkMessage(GameMessage message) {
        switch (message.getType()) {
            case PLAYER_JOINED:
                handlePlayerJoined((PlayerJoinedMessage) message);
                break;
            case GAME_STATE:
                handleGameState((GameStateMessage) message);
                break;
            case CARD_DRAWN:
                handleCardDrawn((CardDrawnMessage) message);
                break;
            case GAME_OVER:
                handleGameOver((GameOverMessage) message);
                break;
            case ERROR:
                handleError((ErrorMessage) message);
                break;
            default:
                logger.warn("Unhandled message type: {}", message.getType());
        }
    }
    
    private void handlePlayerJoined(PlayerJoinedMessage message) {
        if (controlPanel != null) {
            controlPanel.addLogMessage("Player " + message.getPlayerName() + " joined the game!");
        }
    }
    
    private void handleGameState(GameStateMessage message) {
        // Update game state from server
        game.getPlayers().clear();
        for (GameStateMessage.PlayerState playerState : message.getPlayers()) {
            Player player = new Player(playerState.getName());
            player.setPosition(playerState.getPosition());
            game.addPlayer(player);
        }
        
        if (boardPanel != null) {
            boardPanel.repaint();
        }
        
        if (controlPanel != null) {
            controlPanel.updateGameState();
        }
    }
    
    private void handleCardDrawn(CardDrawnMessage message) {
        if (controlPanel != null) {
            controlPanel.addLogMessage(message.getMoveDescription());
        }
    }
    
    private void handleGameOver(GameOverMessage message) {
        if (controlPanel != null) {
            controlPanel.addLogMessage("GAME OVER! " + message.getWinnerName() + " wins!");
        }
        JOptionPane.showMessageDialog(this,
            message.getWinnerName() + " wins the game!",
            "Game Over",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void handleError(ErrorMessage message) {
        JOptionPane.showMessageDialog(this,
            message.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * Checks if this is a multiplayer game.
     */
    public boolean isMultiplayer() {
        return isMultiplayer;
    }
    
    /**
     * Gets the network client (for multiplayer games).
     */
    public GameClient getNetworkClient() {
        return networkClient;
    }
    
    /**
     * Gets the current player's name (for multiplayer games).
     */
    public String getCurrentPlayerName() {
        return currentPlayerName;
    }
}

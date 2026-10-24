package com.example.candyland.network;

/**
 * Message sent by a client to join a game.
 */
public class JoinGameMessage extends GameMessage {
    private static final long serialVersionUID = 2L;
    
    private final String playerName;
    private final String gameRoomId;
    private final String authToken;
    
    public JoinGameMessage(String playerName, String gameRoomId, String authToken) {
        super(MessageType.JOIN_GAME);
        this.playerName = playerName;
        this.gameRoomId = gameRoomId;
        this.authToken = authToken;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public String getGameRoomId() {
        return gameRoomId;
    }
    
    public String getAuthToken() {
        return authToken;
    }
}

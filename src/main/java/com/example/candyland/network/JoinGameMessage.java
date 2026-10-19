package com.example.candyland.network;

/**
 * Message sent by a client to join a game.
 */
public class JoinGameMessage extends GameMessage {
    private static final long serialVersionUID = 1L;
    
    private final String playerName;
    private final String gameRoomId;
    private final String accessCode;
    
    public JoinGameMessage(String playerName, String gameRoomId, String accessCode) {
        super(MessageType.JOIN_GAME);
        this.playerName = playerName;
        this.gameRoomId = gameRoomId;
        this.accessCode = accessCode;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public String getGameRoomId() {
        return gameRoomId;
    }
    
    public String getAccessCode() {
        return accessCode;
    }
}

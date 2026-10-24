package com.example.candyland.network;

/**
 * Message sent when an error occurs.
 */
public class ErrorMessage extends GameMessage {
    private static final long serialVersionUID = 1L;
    
    private final String message;
    private final ErrorCode errorCode;
    
    public enum ErrorCode {
        GAME_FULL,
        INVALID_TURN,
        CONNECTION_ERROR,
        INVALID_PLAYER,
        GAME_NOT_FOUND,
        AUTHENTICATION_FAILED,
        UNKNOWN_ERROR
    }
    
    public ErrorMessage(String message, ErrorCode errorCode) {
        super(MessageType.ERROR);
        this.message = message;
        this.errorCode = errorCode;
    }
    
    public String getMessage() {
        return message;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

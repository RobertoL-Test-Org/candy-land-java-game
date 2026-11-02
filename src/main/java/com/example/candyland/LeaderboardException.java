package com.example.candyland;

/**
 * Thrown when the leaderboard database cannot be read or written.
 */
public class LeaderboardException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new leaderboard exception.
     *
     * @param message the detail message
     * @param cause the underlying cause
     */
    public LeaderboardException(String message, Throwable cause) {
        super(message, cause);
    }
}

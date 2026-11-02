package com.example.candyland;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Persistent leaderboard tracking each player's consecutive wins, stored in an
 * embedded H2 database file.
 * A win extends a player's streak; playing a game without winning resets it to zero.
 * The best streak ever reached is kept so records survive a loss.
 */
public class Leaderboard {
    private static final String DB_SUFFIX = ".mv.db";

    private static final String CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS leaderboard ("
            + "name VARCHAR PRIMARY KEY, "
            + "current_streak INT NOT NULL, "
            + "best_streak INT NOT NULL)";
    private static final String SELECT_ENTRY =
        "SELECT name, current_streak, best_streak FROM leaderboard WHERE name = ? FOR UPDATE";
    private static final String UPSERT_ENTRY =
        "MERGE INTO leaderboard (name, current_streak, best_streak) KEY (name) VALUES (?, ?, ?)";
    private static final String SELECT_RANKINGS =
        "SELECT name, current_streak, best_streak FROM leaderboard "
            + "ORDER BY best_streak DESC, current_streak DESC, LOWER(name), name";
    private static final String COUNT_ENTRIES = "SELECT COUNT(*) FROM leaderboard";

    private final String jdbcUrl;

    /**
     * A single player's leaderboard record.
     *
     * @param name the player's name
     * @param currentStreak consecutive wins in the player's most recent games
     * @param bestStreak the longest win streak the player has reached
     */
    public record Entry(String name, int currentStreak, int bestStreak) {
    }

    /**
     * Creates a leaderboard backed by the given database file, creating it if needed.
     *
     * @param databaseFile the database file path; H2 stores it as {@code <path>.mv.db}
     * @throws LeaderboardException if the database cannot be opened
     */
    public Leaderboard(Path databaseFile) {
        Path base = stripSuffix(databaseFile.toAbsolutePath());
        if (base.toString().contains(";")) {
            throw new IllegalArgumentException("Leaderboard path must not contain ';': " + base);
        }
        try {
            Files.createDirectories(base.getParent());
        } catch (IOException e) {
            throw new LeaderboardException("Could not create leaderboard directory " + base.getParent(), e);
        }
        this.jdbcUrl = "jdbc:h2:file:" + base;
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute(CREATE_TABLE);
        } catch (SQLException e) {
            throw new LeaderboardException("Could not open leaderboard database " + base + DB_SUFFIX, e);
        }
    }

    /**
     * Gets the default leaderboard database location in the user's home directory.
     *
     * @return the default leaderboard database path
     */
    public static Path defaultPath() {
        return Paths.get(System.getProperty("user.home"), ".candyland", "leaderboard");
    }

    /**
     * Records the result of a finished game.
     *
     * @param playerNames the names of everyone who played
     * @param winnerName the name of the winner
     * @throws LeaderboardException if the result cannot be saved
     */
    public void recordGame(List<String> playerNames, String winnerName) {
        Set<String> names = new LinkedHashSet<>();
        for (String name : playerNames) {
            names.add(normalize(name));
        }
        String winner = normalize(winnerName);
        names.add(winner);

        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                for (String name : names) {
                    Entry previous = findEntry(connection, name);
                    int current = previous == null ? 0 : previous.currentStreak();
                    int best = previous == null ? 0 : previous.bestStreak();
                    if (name.equals(winner)) {
                        current++;
                        best = Math.max(best, current);
                    } else {
                        current = 0;
                    }
                    saveEntry(connection, new Entry(name, current, best));
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new LeaderboardException("Could not save game result to the leaderboard", e);
        }
    }

    /**
     * Gets the record for a player.
     *
     * @param name the player's name
     * @return the player's record, or null if they have never played
     * @throws LeaderboardException if the database cannot be read
     */
    public Entry getEntry(String name) {
        try (Connection connection = connect()) {
            return findEntry(connection, normalize(name));
        } catch (SQLException e) {
            throw new LeaderboardException("Could not read the leaderboard", e);
        }
    }

    /**
     * Gets all records ranked by best streak, then current streak, then name.
     *
     * @return the ranked records
     * @throws LeaderboardException if the database cannot be read
     */
    public List<Entry> getRankings() {
        List<Entry> rankings = new ArrayList<>();
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(SELECT_RANKINGS);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                rankings.add(toEntry(rs));
            }
        } catch (SQLException e) {
            throw new LeaderboardException("Could not read the leaderboard", e);
        }
        return rankings;
    }

    /**
     * Checks if any games have been recorded.
     *
     * @return true if the leaderboard has no records
     * @throws LeaderboardException if the database cannot be read
     */
    public boolean isEmpty() {
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(COUNT_ENTRIES);
             ResultSet rs = statement.executeQuery()) {
            return rs.next() && rs.getInt(1) == 0;
        } catch (SQLException e) {
            throw new LeaderboardException("Could not read the leaderboard", e);
        }
    }

    /**
     * Opens a short-lived connection. Connections are not held open between calls
     * so the CLI and GUI can share the same database file.
     */
    private Connection connect() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    private static Entry findEntry(Connection connection, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(SELECT_ENTRY)) {
            statement.setString(1, name);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? toEntry(rs) : null;
            }
        }
    }

    private static void saveEntry(Connection connection, Entry entry) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPSERT_ENTRY)) {
            statement.setString(1, entry.name());
            statement.setInt(2, entry.currentStreak());
            statement.setInt(3, entry.bestStreak());
            statement.executeUpdate();
        }
    }

    private static Entry toEntry(ResultSet rs) throws SQLException {
        return new Entry(rs.getString("name"), rs.getInt("current_streak"), rs.getInt("best_streak"));
    }

    private static String normalize(String name) {
        return name.trim();
    }

    private static Path stripSuffix(Path path) {
        String fileName = path.getFileName().toString();
        if (fileName.endsWith(DB_SUFFIX)) {
            return path.resolveSibling(fileName.substring(0, fileName.length() - DB_SUFFIX.length()));
        }
        return path;
    }
}

package com.example.candyland;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Persistent leaderboard tracking each player's consecutive wins.
 * A win extends a player's streak; playing a game without winning resets it to zero.
 * The best streak ever reached is kept so records survive a loss.
 */
public class Leaderboard {
    private final Path file;
    private final Map<String, Entry> entries;

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
     * Creates a leaderboard backed by the given file, loading any saved records.
     *
     * @param file the file used to persist the leaderboard
     */
    public Leaderboard(Path file) {
        this.file = file;
        this.entries = new HashMap<>();
        load();
    }

    /**
     * Gets the default leaderboard location in the user's home directory.
     *
     * @return the default leaderboard file path
     */
    public static Path defaultPath() {
        return Paths.get(System.getProperty("user.home"), ".candyland", "leaderboard.properties");
    }

    /**
     * Records the result of a finished game and saves the leaderboard.
     *
     * @param playerNames the names of everyone who played
     * @param winnerName the name of the winner
     */
    public void recordGame(List<String> playerNames, String winnerName) {
        Set<String> names = new LinkedHashSet<>();
        for (String name : playerNames) {
            names.add(normalize(name));
        }
        String winner = normalize(winnerName);
        names.add(winner);

        for (String name : names) {
            Entry previous = entries.getOrDefault(name, new Entry(name, 0, 0));
            if (name.equals(winner)) {
                int streak = previous.currentStreak() + 1;
                entries.put(name, new Entry(name, streak, Math.max(streak, previous.bestStreak())));
            } else {
                entries.put(name, new Entry(name, 0, previous.bestStreak()));
            }
        }
        save();
    }

    /**
     * Gets the record for a player.
     *
     * @param name the player's name
     * @return the player's record, or null if they have never played
     */
    public Entry getEntry(String name) {
        return entries.get(normalize(name));
    }

    /**
     * Gets all records ranked by best streak, then current streak, then name.
     *
     * @return the ranked records
     */
    public List<Entry> getRankings() {
        List<Entry> rankings = new ArrayList<>(entries.values());
        rankings.sort(Comparator.comparingInt(Entry::bestStreak).reversed()
            .thenComparing(Comparator.comparingInt(Entry::currentStreak).reversed())
            .thenComparing(Entry::name, String.CASE_INSENSITIVE_ORDER));
        return rankings;
    }

    /**
     * Checks if any games have been recorded.
     *
     * @return true if the leaderboard has no records
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    private static String normalize(String name) {
        return name.trim();
    }

    private void load() {
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (NoSuchFileException e) {
            return;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read leaderboard " + file, e);
        }
        for (String name : props.stringPropertyNames()) {
            String[] parts = props.getProperty(name).split(",");
            try {
                int current = Integer.parseInt(parts[0].trim());
                int best = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : current;
                entries.put(name, new Entry(name, current, Math.max(current, best)));
            } catch (NumberFormatException e) {
                // skip malformed records rather than failing the whole leaderboard
            }
        }
    }

    private void save() {
        Properties props = new Properties();
        for (Entry entry : entries.values()) {
            props.setProperty(entry.name(), entry.currentStreak() + "," + entry.bestStreak());
        }
        try {
            Path parent = file.toAbsolutePath().getParent();
            Files.createDirectories(parent);
            Path temp = Files.createTempFile(parent, "leaderboard", ".tmp");
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
                props.store(writer, "Candy Land leaderboard: name=currentStreak,bestStreak");
            }
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save leaderboard " + file, e);
        }
    }
}

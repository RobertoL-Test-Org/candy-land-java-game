package com.example.candyland;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Unit tests for the Leaderboard class.
 */
class LeaderboardTest {
    @TempDir
    Path tempDir;

    private Path file() {
        return tempDir.resolve("nested").resolve("leaderboard.properties");
    }

    @Test
    void testNewLeaderboardIsEmpty() {
        Leaderboard leaderboard = new Leaderboard(file());
        assertTrue(leaderboard.isEmpty());
        assertNull(leaderboard.getEntry("Alice"));
    }

    @Test
    void testConsecutiveWinsBuildStreak() {
        Leaderboard leaderboard = new Leaderboard(file());
        leaderboard.recordGame(List.of("Alice", "Bob"), "Alice");
        leaderboard.recordGame(List.of("Alice", "Bob"), "Alice");
        leaderboard.recordGame(List.of("Alice", "Bob"), "Alice");

        assertEquals(new Leaderboard.Entry("Alice", 3, 3), leaderboard.getEntry("Alice"));
        assertEquals(new Leaderboard.Entry("Bob", 0, 0), leaderboard.getEntry("Bob"));
    }

    @Test
    void testLossResetsCurrentStreakButKeepsBest() {
        Leaderboard leaderboard = new Leaderboard(file());
        leaderboard.recordGame(List.of("Alice", "Bob"), "Alice");
        leaderboard.recordGame(List.of("Alice", "Bob"), "Alice");
        leaderboard.recordGame(List.of("Alice", "Bob"), "Bob");

        assertEquals(new Leaderboard.Entry("Alice", 0, 2), leaderboard.getEntry("Alice"));
        assertEquals(new Leaderboard.Entry("Bob", 1, 1), leaderboard.getEntry("Bob"));
    }

    @Test
    void testPlayersNotInGameAreUnaffected() {
        Leaderboard leaderboard = new Leaderboard(file());
        leaderboard.recordGame(List.of("Alice", "Bob"), "Alice");
        leaderboard.recordGame(List.of("Bob", "Carol"), "Carol");

        assertEquals(new Leaderboard.Entry("Alice", 1, 1), leaderboard.getEntry("Alice"));
    }

    @Test
    void testRecordsPersistAcrossInstances() {
        Leaderboard first = new Leaderboard(file());
        first.recordGame(List.of("Alice", "Bob"), "Alice");
        first.recordGame(List.of("Alice", "Bob"), "Alice");

        Leaderboard reloaded = new Leaderboard(file());
        assertEquals(new Leaderboard.Entry("Alice", 2, 2), reloaded.getEntry("Alice"));

        reloaded.recordGame(List.of("Alice", "Bob"), "Alice");
        assertEquals(3, new Leaderboard(file()).getEntry("Alice").currentStreak());
    }

    @Test
    void testNamesWithSpecialCharactersPersist() {
        Leaderboard leaderboard = new Leaderboard(file());
        leaderboard.recordGame(List.of("Mr. Mint = #1", "Señora: Lolly"), "Mr. Mint = #1");

        Leaderboard reloaded = new Leaderboard(file());
        assertEquals(1, reloaded.getEntry("Mr. Mint = #1").currentStreak());
        assertEquals(0, reloaded.getEntry("Señora: Lolly").currentStreak());
    }

    @Test
    void testNamesAreTrimmed() {
        Leaderboard leaderboard = new Leaderboard(file());
        leaderboard.recordGame(List.of("  Alice ", "Bob"), "Alice");
        assertEquals(1, leaderboard.getEntry(" Alice").currentStreak());
    }

    @Test
    void testRankingsOrderedByBestThenCurrentThenName() {
        Leaderboard leaderboard = new Leaderboard(file());
        leaderboard.recordGame(List.of("Alice", "Bob"), "Alice");
        leaderboard.recordGame(List.of("Alice", "Bob"), "Alice");
        leaderboard.recordGame(List.of("Alice", "Bob"), "Bob");
        leaderboard.recordGame(List.of("Carol", "Dave"), "Carol");
        leaderboard.recordGame(List.of("Carol", "Dave"), "Carol");

        List<String> order = leaderboard.getRankings().stream().map(Leaderboard.Entry::name).toList();
        assertEquals(List.of("Carol", "Alice", "Bob", "Dave"), order);
    }

    @Test
    void testMalformedLinesAreSkipped() throws IOException {
        Files.createDirectories(file().getParent());
        Files.writeString(file(), "Alice=2,5\nBob=oops\n", StandardCharsets.UTF_8);

        Leaderboard leaderboard = new Leaderboard(file());
        assertEquals(new Leaderboard.Entry("Alice", 2, 5), leaderboard.getEntry("Alice"));
        assertNull(leaderboard.getEntry("Bob"));
    }
}

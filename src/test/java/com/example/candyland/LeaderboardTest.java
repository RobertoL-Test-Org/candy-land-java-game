package com.example.candyland;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

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
        return tempDir.resolve("nested").resolve("leaderboard");
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
    void testDatabaseFileIsCreated() {
        new Leaderboard(file());
        assertTrue(Files.exists(tempDir.resolve("nested").resolve("leaderboard.mv.db")));
    }

    @Test
    void testDatabaseSuffixIsOptional() {
        new Leaderboard(file()).recordGame(List.of("Alice", "Bob"), "Alice");
        Leaderboard withSuffix = new Leaderboard(tempDir.resolve("nested").resolve("leaderboard.mv.db"));
        assertEquals(1, withSuffix.getEntry("Alice").currentStreak());
    }

    @Test
    void testInstancesSharingFileSeeEachOthersWrites() {
        Leaderboard cli = new Leaderboard(file());
        Leaderboard gui = new Leaderboard(file());
        cli.recordGame(List.of("Alice", "Bob"), "Alice");
        gui.recordGame(List.of("Alice", "Bob"), "Alice");
        assertEquals(2, cli.getEntry("Alice").currentStreak());
    }

    @Test
    void testPathWithSemicolonIsRejected() {
        Path bad = tempDir.resolve("evil;INIT=RUNSCRIPT FROM 'x'");
        assertThrows(IllegalArgumentException.class, () -> new Leaderboard(bad));
    }

    @Test
    void testLongNamesAreStored() {
        String longName = "x".repeat(500);
        Leaderboard leaderboard = new Leaderboard(file());
        leaderboard.recordGame(List.of(longName, "Bob"), longName);
        assertEquals(1, leaderboard.getEntry(longName).currentStreak());
    }
}

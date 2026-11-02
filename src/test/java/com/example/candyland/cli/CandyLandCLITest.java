package com.example.candyland.cli;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Unit tests for the CandyLandCLI class.
 */
class CandyLandCLITest {

    private String run(String input) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        new CandyLandCLI(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), out, false).run();
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    void testInvalidPlayerCountIsRejected() {
        String output = run("0\nabc\n4\n1\nAlice\nq\n");
        assertEquals(3, output.split("Please enter a number between 1 and 3", -1).length - 1);
        assertTrue(output.contains("Alice"));
        assertTrue(output.contains("Game abandoned."));
    }

    @Test
    void testBlankNameGetsDefault() {
        String output = run("2\n\nBob\nq\n");
        assertTrue(output.contains("Player 1"));
        assertTrue(output.contains("Bob"));
    }

    @Test
    void testSinglePlayerGameRunsToCompletion() {
        String output = run("1\nSolo\n" + "\n".repeat(500) + "n\n");
        assertTrue(output.contains("Solo reaches King Kandy's Castle and wins!"));
        assertTrue(output.contains("Thanks for playing Candy Land!"));
    }

    @Test
    void testBoardCommandPrintsBoard() {
        String output = run("1\nAlice\nb\nq\n");
        int boards = output.split("=== Candy Land Board ===", -1).length - 1;
        assertEquals(2, boards);
    }

    @Test
    void testEndOfInputExitsCleanly() {
        String output = run("2\nAlice\n");
        assertTrue(output.contains("Thanks for playing Candy Land!"));
    }
}

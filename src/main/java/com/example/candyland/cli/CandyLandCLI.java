package com.example.candyland.cli;

import com.example.candyland.Board;
import com.example.candyland.CandyLandGame;
import com.example.candyland.Card;
import com.example.candyland.Color;
import com.example.candyland.Leaderboard;
import com.example.candyland.LeaderboardException;
import com.example.candyland.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Command-line interface for the Candy Land game.
 * Renders the board as text and lets 1-3 players take turns from the terminal.
 */
public class CandyLandCLI {
    private static final int MAX_PLAYERS = 3;
    private static final int SPACES_PER_ROW = 27;
    private static final int PROGRESS_BAR_WIDTH = 30;
    private static final String[] CHARACTERS = {
        "Plumpy", "Mr. Mint", "Jolly", "Lord Licorice",
        "Gramma Nutt", "Princess Lolly", "Queen Frostine", "King Kandy"
    };

    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String[] PLAYER_STYLES = {
        "\u001B[1;97;41m", "\u001B[1;97;44m", "\u001B[1;30;43m"
    };

    private final BufferedReader in;
    private final PrintStream out;
    private final boolean useColor;
    private final Leaderboard leaderboard;

    /**
     * Creates a new CLI bound to the given streams.
     *
     * @param in the input stream to read commands from
     * @param out the output stream to write to
     * @param useColor whether to emit ANSI color codes
     * @param leaderboard the leaderboard that records win streaks
     */
    public CandyLandCLI(InputStream in, PrintStream out, boolean useColor, Leaderboard leaderboard) {
        this.in = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        this.out = out;
        this.useColor = useColor;
        this.leaderboard = leaderboard;
    }

    /**
     * Runs games until the players choose to stop or input ends.
     */
    public void run() {
        printBanner();
        if (hasRecords()) {
            printLeaderboard();
        }
        boolean playAgain = true;
        while (playAgain) {
            CandyLandGame game = setupGame();
            if (game == null) {
                break;
            }
            boolean finished = playGame(game);
            playAgain = finished && askYesNo("Play again? (y/n): ");
        }
        out.println("Thanks for playing Candy Land!");
    }

    private boolean hasRecords() {
        try {
            return !leaderboard.isEmpty();
        } catch (LeaderboardException e) {
            out.println("Warning: " + e.getMessage());
            return false;
        }
    }

    /**
     * Prompts for player count and names.
     *
     * @return a configured game, or null if input ended
     */
    CandyLandGame setupGame() {
        Integer numPlayers = askPlayerCount();
        if (numPlayers == null) {
            return null;
        }
        CandyLandGame game = new CandyLandGame();
        for (int i = 1; i <= numPlayers; i++) {
            String name = prompt("Name for Player " + i + ": ");
            if (name == null) {
                return null;
            }
            game.addPlayer(name.isBlank() ? "Player " + i : name.trim());
        }
        out.println();
        return game;
    }

    /**
     * Plays turns until someone wins or the players quit.
     *
     * @param game the game to play
     * @return true if the game finished with a winner
     */
    boolean playGame(CandyLandGame game) {
        printBoard(game);
        while (game.getWinner() == null) {
            Player current = game.getCurrentPlayer();
            String command = prompt(styleFor(game, current)
                + "'s turn - [Enter] draw, [b] board, [l] leaderboard, [q] quit: ");
            if (command == null || command.trim().equalsIgnoreCase("q")) {
                out.println("Game abandoned.");
                return false;
            }
            if (command.trim().equalsIgnoreCase("b")) {
                printBoard(game);
                continue;
            }
            if (command.trim().equalsIgnoreCase("l")) {
                printLeaderboard();
                continue;
            }
            takeTurn(game, current);
        }
        out.println();
        printBoard(game);
        out.println(bold("*** " + game.getWinner().getName() + " reaches King Kandy's Castle and wins! ***"));
        recordResult(game);
        return true;
    }

    /**
     * Records a finished game on the leaderboard and prints the standings.
     * Solo games are not recorded, since a single player always wins.
     *
     * @param game the finished game
     */
    void recordResult(CandyLandGame game) {
        List<Player> players = game.getPlayers();
        if (players.size() < 2) {
            out.println("(Solo games don't count toward the leaderboard.)");
            out.println();
            return;
        }
        List<String> names = new ArrayList<>();
        for (Player player : players) {
            names.add(player.getName());
        }
        try {
            leaderboard.recordGame(names, game.getWinner().getName());
            Leaderboard.Entry entry = leaderboard.getEntry(game.getWinner().getName());
            out.println(game.getWinner().getName() + " is on a " + entry.currentStreak()
                + "-game win streak!");
        } catch (LeaderboardException e) {
            out.println("Warning: " + e.getMessage());
            return;
        }
        out.println();
        printLeaderboard();
    }

    /**
     * Prints the leaderboard ranked by best win streak.
     */
    void printLeaderboard() {
        out.println(bold("=== Leaderboard (consecutive wins) ==="));
        List<Leaderboard.Entry> rankings;
        try {
            rankings = leaderboard.getRankings();
        } catch (LeaderboardException e) {
            out.println("  Warning: " + e.getMessage());
            out.println();
            return;
        }
        if (rankings.isEmpty()) {
            out.println("  No games recorded yet.");
        } else {
            out.printf("  %-4s %-20s %7s %7s%n", "Rank", "Player", "Current", "Best");
            for (int i = 0; i < rankings.size(); i++) {
                Leaderboard.Entry entry = rankings.get(i);
                out.printf("  %-4d %-20s %7d %7d%n", i + 1, entry.name(), entry.currentStreak(), entry.bestStreak());
            }
        }
        out.println();
    }

    /**
     * Draws a card for the player and moves them.
     *
     * @param game the game in progress
     * @param player the player taking the turn
     */
    void takeTurn(CandyLandGame game, Player player) {
        Board board = game.getBoard();
        Card card = game.getDeck().drawCard();
        int from = player.getPosition();
        int to = game.calculateNewPosition(player, card);
        player.setPosition(to);

        out.println("  Drew: " + describeCard(card));
        if (to == from) {
            out.println("  " + player.getName() + " stays at space " + from + ".");
        } else {
            out.println("  " + player.getName() + " moves from space " + from + " to space " + to
                + " (" + (to > from ? "+" : "") + (to - from) + ").");
        }
        out.println("  " + progressBar(to, board.getWinningPosition()));

        if (board.isWinningSpace(to)) {
            game.setWinner(player);
        } else {
            game.nextPlayer();
        }
    }

    /**
     * Prints the board as rows of colored spaces with player markers.
     *
     * @param game the game whose board to print
     */
    void printBoard(CandyLandGame game) {
        Board board = game.getBoard();
        List<Player> players = game.getPlayers();
        int size = board.getBoardSize();

        out.println(bold("=== Candy Land Board ==="));
        for (int rowStart = 0; rowStart < size; rowStart += SPACES_PER_ROW) {
            int rowEnd = Math.min(rowStart + SPACES_PER_ROW, size);
            StringBuilder row = new StringBuilder(String.format("%3d ", rowStart));
            for (int pos = rowStart; pos < rowEnd; pos++) {
                row.append(renderSpace(board, players, pos));
            }
            row.append(String.format(" %3d", rowEnd - 1));
            out.println(row);
        }
        out.println();
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            String marker = playerMarker(i);
            out.printf("  %s %-15s space %3d  %s%n", marker, p.getName(), p.getPosition(),
                progressBar(p.getPosition(), board.getWinningPosition()));
        }
        out.println(useColor
            ? "  Legend: spaces show their color; numbers are players; * marks a character space"
            : "  Legend: R/P/Y/B/O/G = space color; 1-3 = players; * = character space; # = finish");
        out.println();
    }

    private String renderSpace(Board board, List<Player> players, int pos) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getPosition() == pos) {
                return playerMarker(i);
            }
        }
        if (pos == board.getWinningPosition()) {
            return useColor ? "\u001B[1;30;47m#" + RESET : "#";
        }
        String symbol = isCharacterSpace(board, pos) ? "*" : colorLetter(board.getSpaceColor(pos));
        return useColor ? ansiBackground(board.getSpaceColor(pos)) + symbol + RESET : symbol;
    }

    private boolean isCharacterSpace(Board board, int pos) {
        for (String character : CHARACTERS) {
            if (board.getSpecialCharacterPosition(character) == pos) {
                return true;
            }
        }
        return false;
    }

    private String describeCard(Card card) {
        if (card.isSpecialCharacter()) {
            return bold(card.getSpecialCharacter()) + " card!";
        }
        String label = card.isDouble() ? "Double " + card.getColor() : card.getColor().toString();
        return useColor ? ansiBackground(card.getColor()) + " " + label + " " + RESET : label;
    }

    private String progressBar(int position, int finish) {
        int filled = (int) Math.round((double) position / finish * PROGRESS_BAR_WIDTH);
        int percent = (int) Math.round((double) position / finish * 100);
        return "[" + "=".repeat(filled) + " ".repeat(PROGRESS_BAR_WIDTH - filled) + "] " + percent + "%";
    }

    private String playerMarker(int index) {
        String number = String.valueOf(index + 1);
        return useColor ? PLAYER_STYLES[index % PLAYER_STYLES.length] + number + RESET : number;
    }

    private String styleFor(CandyLandGame game, Player player) {
        int index = game.getPlayers().indexOf(player);
        return playerMarker(index) + " " + bold(player.getName());
    }

    private static String colorLetter(Color color) {
        return color.name().substring(0, 1);
    }

    private static String ansiBackground(Color color) {
        switch (color) {
            case RED: return "\u001B[30;41m";
            case PURPLE: return "\u001B[30;45m";
            case YELLOW: return "\u001B[30;103m";
            case BLUE: return "\u001B[30;44m";
            case ORANGE: return "\u001B[30;43m";
            case GREEN: return "\u001B[30;42m";
            default: return "";
        }
    }

    private String bold(String text) {
        return useColor ? BOLD + text + RESET : text;
    }

    private void printBanner() {
        out.println(bold("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"));
        out.println(bold("      Welcome to Candy Land!"));
        out.println(bold("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"));
        out.println("Draw cards to move to the next space of that color.");
        out.println("Character cards send you to their space (never backward).");
        out.println("First to reach the end wins!");
        out.println();
    }

    private Integer askPlayerCount() {
        while (true) {
            String line = prompt("Number of players (1-" + MAX_PLAYERS + "): ");
            if (line == null) {
                return null;
            }
            try {
                int count = Integer.parseInt(line.trim());
                if (count >= 1 && count <= MAX_PLAYERS) {
                    return count;
                }
            } catch (NumberFormatException e) {
                // fall through to the error message
            }
            out.println("Please enter a number between 1 and " + MAX_PLAYERS + ".");
        }
    }

    private boolean askYesNo(String question) {
        while (true) {
            String line = prompt(question);
            if (line == null) {
                return false;
            }
            String answer = line.trim().toLowerCase();
            if (answer.startsWith("y")) {
                return true;
            }
            if (answer.startsWith("n")) {
                return false;
            }
        }
    }

    private String prompt(String message) {
        out.print(message);
        out.flush();
        try {
            return in.readLine();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Entry point for the command-line version of the game.
     *
     * @param args pass {@code --no-color} to disable ANSI colors, or
     *             {@code --leaderboard <file>} to use a different leaderboard file
     */
    public static void main(String[] args) {
        boolean color = System.console() != null && System.getenv("NO_COLOR") == null;
        Path leaderboardFile = Leaderboard.defaultPath();
        for (int i = 0; i < args.length; i++) {
            if ("--no-color".equals(args[i])) {
                color = false;
            } else if ("--color".equals(args[i])) {
                color = true;
            } else if ("--leaderboard".equals(args[i]) && i + 1 < args.length) {
                leaderboardFile = Paths.get(args[++i]);
            }
        }
        Leaderboard leaderboard;
        try {
            leaderboard = new Leaderboard(leaderboardFile);
        } catch (LeaderboardException | IllegalArgumentException e) {
            System.err.println("Could not open the leaderboard: " + e.getMessage());
            System.exit(1);
            return;
        }
        new CandyLandCLI(System.in, System.out, color, leaderboard).run();
    }
}

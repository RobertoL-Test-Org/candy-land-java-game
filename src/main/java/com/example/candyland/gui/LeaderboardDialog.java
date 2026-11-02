package com.example.candyland.gui;

import com.example.candyland.Leaderboard;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Dialog showing the persistent win-streak leaderboard.
 */
public class LeaderboardDialog extends JDialog {
    private static final String[] COLUMNS = {"Rank", "Player", "Current Streak", "Best Streak"};

    /**
     * Creates a leaderboard dialog.
     *
     * @param parent the parent frame
     * @param rankings the ranked leaderboard entries to display
     * @param highlightName a player name to select in the table, or null
     */
    public LeaderboardDialog(Frame parent, List<Leaderboard.Entry> rankings, String highlightName) {
        super(parent, "Leaderboard - Consecutive Wins", true);
        initializeComponents(rankings, highlightName);
        pack();
        setLocationRelativeTo(parent);
    }

    /**
     * Initializes the dialog components.
     */
    private void initializeComponents(List<Leaderboard.Entry> rankings, String highlightName) {
        setLayout(new BorderLayout(10, 10));

        JLabel titleLabel = new JLabel("Candy Land Leaderboard", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        add(titleLabel, BorderLayout.NORTH);

        if (rankings.isEmpty()) {
            JLabel emptyLabel = new JLabel("No games recorded yet. Win a game with 2 or more players!",
                SwingConstants.CENTER);
            emptyLabel.setBorder(BorderFactory.createEmptyBorder(30, 20, 30, 20));
            add(emptyLabel, BorderLayout.CENTER);
        } else {
            add(new JScrollPane(createTable(rankings, highlightName)), BorderLayout.CENTER);
        }

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(closeButton);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Creates a read-only table of the rankings.
     */
    private JTable createTable(List<Leaderboard.Entry> rankings, String highlightName) {
        DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return column == 1 ? String.class : Integer.class;
            }
        };
        int highlightRow = -1;
        for (int i = 0; i < rankings.size(); i++) {
            Leaderboard.Entry entry = rankings.get(i);
            model.addRow(new Object[] {i + 1, entry.name(), entry.currentStreak(), entry.bestStreak()});
            if (entry.name().equals(highlightName)) {
                highlightRow = i;
            }
        }

        JTable table = new JTable(model);
        table.setRowHeight(24);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(110);
        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        table.setPreferredScrollableViewportSize(
            new Dimension(470, Math.min(rankings.size(), 10) * table.getRowHeight()));
        if (highlightRow >= 0) {
            table.setRowSelectionInterval(highlightRow, highlightRow);
        }
        return table;
    }
}

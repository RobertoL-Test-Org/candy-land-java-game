package com.example.candyland.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for connecting to a multiplayer game server.
 */
public class MultiplayerConnectionDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final String VALIDATION_ERROR = "Validation Error";
    
    private final JTextField serverHostField;
    private final JSpinner serverPortSpinner;
    private final JTextField playerNameField;
    private final JTextField roomIdField;
    private final JPasswordField serverPasswordField = new JPasswordField(20);
    private boolean confirmed;
    
    public MultiplayerConnectionDialog(Frame parent) {
        super(parent, "Connect to Multiplayer Game", true);
        
        // Initialize fields
        serverHostField = new JTextField("localhost", 20);
        serverPortSpinner = new JSpinner(new SpinnerNumberModel(8888, 1, 65535, 1));
        playerNameField = new JTextField(20);
        roomIdField = new JTextField("default", 20);
        confirmed = false;
        
        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Create form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Server host
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Server Host:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        formPanel.add(serverHostField, gbc);
        
        // Server port
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Server Port:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        formPanel.add(serverPortSpinner, gbc);
        
        // Player name
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Your Name:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        formPanel.add(playerNameField, gbc);
        
        // Room ID
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Room ID:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        formPanel.add(roomIdField, gbc);
        
        // Server password
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Server Password:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        formPanel.add(serverPasswordField, gbc);
        
        mainPanel.add(formPanel, BorderLayout.CENTER);
        
        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton connectButton = new JButton("Connect");
        JButton cancelButton = new JButton("Cancel");
        
        connectButton.addActionListener(e -> {
            if (validateInput()) {
                confirmed = true;
                dispose();
            }
        });
        
        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });
        
        buttonPanel.add(connectButton);
        buttonPanel.add(cancelButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Add info panel
        JPanel infoPanel = new JPanel(new BorderLayout());
        JLabel infoLabel = new JLabel("<html><b>Multiplayer Game</b><br>" +
            "Enter the server details and your player name to join a game.<br>" +
            "Use the same Room ID to play with friends!</html>");
        infoPanel.add(infoLabel, BorderLayout.CENTER);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        mainPanel.add(infoPanel, BorderLayout.NORTH);
        
        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
    
    private boolean validateInput() {
        if (playerNameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter your name",
                VALIDATION_ERROR,
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        if (serverHostField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter server host",
                VALIDATION_ERROR,
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        if (roomIdField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter room ID",
                VALIDATION_ERROR,
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        if (serverPasswordField.getPassword().length == 0) {
            JOptionPane.showMessageDialog(this,
                "Please enter server password",
                VALIDATION_ERROR,
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        return true;
    }
    
    public boolean isConfirmed() {
        return confirmed;
    }
    
    public String getServerHost() {
        return serverHostField.getText().trim();
    }
    
    public int getServerPort() {
        return (Integer) serverPortSpinner.getValue();
    }
    
    public String getPlayerName() {
        return playerNameField.getText().trim();
    }
    
    public String getRoomId() {
        return roomIdField.getText().trim();
    }
    
    public String getServerPassword() {
        return new String(serverPasswordField.getPassword());
    }
}

package ru.porfirevalexey.networkchat.client.gui;

import javax.swing.*;
import java.awt.*;

public class NetworkChatClientGUI extends JFrame {

    private JList usersList;
    private JTextField userMessagetextField;
    private JTextArea chatArea;
    private JLabel userName;
    private JPanel mainPanel;

    public NetworkChatClientGUI(String title) {
        super(title);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setContentPane(mainPanel);
        createUIComponents();
        this.pack();
    }

    private void createUIComponents() {
        mainPanel.setPreferredSize(new Dimension(640, 480));
        this.setLocation(500, 500);
        this.setResizable(false);       // TODO: place custom component creation code here
    }

    public static void main(String[] args) {
        JFrame frame = new NetworkChatClientGUI("Client App");
        frame.setVisible(true);
    }
}

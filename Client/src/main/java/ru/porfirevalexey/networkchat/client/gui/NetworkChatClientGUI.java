package ru.porfirevalexey.networkchat.client.gui;

import ru.porfirevalexey.networkchat.connection.Connection;
import ru.porfirevalexey.networkchat.connection.ConnectionListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;


public class NetworkChatClientGUI extends JFrame implements ActionListener, ConnectionListener {

    private JList usersList;
    private JTextField userMessagetextField;
    private JTextArea chatArea;
    private JLabel userName;
    private JPanel mainPanel;

    private static Connection conn;


    public NetworkChatClientGUI(String title) throws IOException {
        super(title);
        conn = new Connection(this);
        conn.connectToServer();

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setContentPane(mainPanel);
        createUIComponents();
        this.pack();

        new Thread(null, conn::receiveMessageFromServer, "receiveMessages").start();
    }

    private void createUIComponents() {
        mainPanel.setPreferredSize(new Dimension(640, 480));
        this.setLocation(500, 500);
        this.setResizable(false);
        this.setLocationRelativeTo(null);
        this.setAlwaysOnTop(true);

        this.userMessagetextField.addActionListener(this);
        this.userName.setText(conn.getUserName());
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        JFrame frame = new NetworkChatClientGUI("Client App");
        frame.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String message = this.userMessagetextField.getText();
        if("".equals(message)) return;
        this.userMessagetextField.setText(null);
        conn.sendMessageToServer(message);
    }

    @Override
    public void newMessageReceived(String msg) {
        this.chatArea.append(msg + "\n");
    }

    @Override
    public void newConnectionEstablished() {
        this.chatArea.setText("Соединение с сервером установлено...");
    }

    @Override
    public void disconnected() {
        this.chatArea.setText("Соединение с сервером разорвано...");
    }
}

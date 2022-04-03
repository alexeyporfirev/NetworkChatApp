package ru.porfirevalexey.networkchat.client.gui;

import ru.porfirevalexey.networkchat.connection.Connection;
import ru.porfirevalexey.networkchat.connection.ConnectionListener;
import ru.porfirevalexey.networkchat.message.Message;
import ru.porfirevalexey.networkchat.message.MessageMode;
import ru.porfirevalexey.networkchat.message.MessageType;
import ru.porfirevalexey.networkchat.message.TextMessage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.util.*;


public class NetworkChatClientGUI extends JFrame implements ActionListener, ConnectionListener {

    private JList usersList;
    private JTextField userMessagetextField;
    private JTextArea chatArea;
    private JLabel userName;
    private JPanel mainPanel;
    private JScrollPane scrollPanel;

    private static Connection conn;
    private SortedSet<String> users;
    private static Thread threadReceiver;


    public NetworkChatClientGUI(String title) throws IOException {
        super(title);
        conn = new Connection(this);
        conn.connectToServer();

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setContentPane(mainPanel);
        createUIComponents();
        this.pack();
        this.setVisible(true);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    if (conn.isConnected()) {
                        conn.disconnectFromServer();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        threadReceiver = new Thread(null, conn::receiveMessageFromServer, "receiveMessages");
        threadReceiver.start();
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

    public static void main(String[] args) throws IOException, InterruptedException, InvocationTargetException {
        new NetworkChatClientGUI("Client App");

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String message = this.userMessagetextField.getText();
        if ("".equals(message)) return;
        Message msg = new TextMessage(conn.getUserName(), message);
        conn.sendMessageToServer(msg);
        this.userMessagetextField.setText(null);
    }

    @Override
    public void newMessageReceived(Message msg) {
        if (msg.getMessageType() == MessageType.TEXT) {
            this.chatArea.append(generateMessageForChat(msg.toString()));
            this.chatArea.setCaretPosition(this.chatArea.getDocument().getLength());
        }
    }

    @Override
    public void newConnectionEstablished() {
        this.chatArea.append("Соединение с сервером установлено..." + "\n");
        this.chatArea.setCaretPosition(this.chatArea.getDocument().getLength());
        this.chatArea.setCaretPosition(this.chatArea.getDocument().getLength());

    }

    @Override
    public void disconnected() {
        this.chatArea.append("Соединение с сервером разорвано..." + "\n");
        this.chatArea.setCaretPosition(this.chatArea.getDocument().getLength());
        threadReceiver.interrupt();
    }

    @Override
    public void newUserAdded(String userName) {
        chatArea.append(generateMessageForChat(userName + " присоединился к чату"));
        this.chatArea.setCaretPosition(this.chatArea.getDocument().getLength());
        usersList.setListData(conn.getUsers().toArray());
    }

    @Override
    public void userWasRemoved(String userName) {
        chatArea.append(generateMessageForChat(userName + " покинул чат"));
        this.chatArea.setCaretPosition(this.chatArea.getDocument().getLength());
        usersList.setListData(conn.getUsers().toArray());
    }

    @Override
    public void userNameWasChanged(String oldUserName, String newUserName) {
        chatArea.append(generateMessageForChat(oldUserName + " сменил ник на " + newUserName));
        this.chatArea.setCaretPosition(this.chatArea.getDocument().getLength());
        usersList.setListData(conn.getUsers().toArray());
        if(this.userName.getText().equals(oldUserName)) {
            this.userName.setText(conn.getUserName());
        }
    }

    @Override
    public void userNameWasNotChanged(String oldUserName, String newUserName) {
        chatArea.append(generateMessageForChat(newUserName + " - такой ник уже существует! Выберите другой ник"));
    }

    @Override
    public void userListReceived(Set<String> users) {
        usersList.setListData(conn.getUsers().toArray());
    }

    private String generateMessageForChat(String message) {
        return "[" + new Timestamp(System.currentTimeMillis()) + "]" + message + "\n";
    }
}

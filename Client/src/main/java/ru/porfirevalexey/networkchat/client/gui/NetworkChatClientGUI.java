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
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.Time;
import java.util.*;


public class NetworkChatClientGUI extends JFrame implements ActionListener, ConnectionListener {

    private JList usersList;
    private JTextField userMessagetextField;
    private JTextArea chatArea;
    private JLabel userName;
    private JPanel mainPanel;

    private static Connection conn;
    private SortedSet<String> users;
    private static Thread threadReceiver;


    public NetworkChatClientGUI(String title) throws IOException {
        super(title);
        users = new TreeSet<String>();
        conn = new Connection(this);
        conn.connectToServer();

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setContentPane(mainPanel);
        createUIComponents();
        this.pack();
        this.setVisible(true);
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

    private boolean isItCorrectedUserName(String newUserName) {
        return !users.contains(newUserName);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String message = this.userMessagetextField.getText();
        if ("".equals(message)) return;
        this.userMessagetextField.setText(null);
        Message msg = new TextMessage(conn.getUserName(), message);
        conn.sendMessageToServer(msg);
    }

    @Override
    public void newMessageReceived(Message msg) {
        if (msg.getMessageMode() == MessageMode.STANDARD) {
            if (msg.getMessageType() == MessageType.TEXT) {
                this.chatArea.append(new String("[" + new Time(System.currentTimeMillis()) + "] " + msg.getUsername() + ": " + new String(msg.getContent())) + "\n");
            }
        } else {
            System.out.println("!!!!!!!!" + new String(msg.getContent()));
            switch ((new String(msg.getContent())).split("\\s+")[0]) {
                case "\\exit":
                    System.out.println("EXIT");
                    users.remove(msg.getUsername());
                    usersList.setListData(users.toArray());
                    break;
                case "\\changeName":
                    System.out.println("CHANGE_NAME");
                    System.out.println(users);
                    System.out.println(msg.getUsername());
                    users.remove(msg.getUsername());
                    users.add((new String(msg.getContent())).split("\\s+")[1]);
                    usersList.setListData(users.toArray());
                    break;
                case "\\newUser":
                    System.out.println("NEW_USER");
                    users.add(msg.getUsername());
                    System.out.println(users);
                    usersList.setListData(users.toArray());
                    break;
                case "\\errorUserName":
                    chatArea.append("Такое имя пользователя уже существует!");
                case "\\userList":
                    System.out.println("USER_LIST");
                    String[] usersFromServer = (new String(msg.getContent())).split("\\s+")[1].split(":");
                    System.out.println(msg.getUsername());
                    users.addAll(Arrays.asList(usersFromServer));
                    usersList.setListData(usersFromServer);
                    break;
            }
        }

    }

    @Override
    public void newConnectionEstablished() {
        this.chatArea.append("Соединение с сервером установлено..." + "\n");
    }

    @Override
    public void disconnected() {
        try {
            conn.disconnectFromServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.chatArea.append("Соединение с сервером разорвано..." + "\n");
        threadReceiver.interrupt();
    }
}

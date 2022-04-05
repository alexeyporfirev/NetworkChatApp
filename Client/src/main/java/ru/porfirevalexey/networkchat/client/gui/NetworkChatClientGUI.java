package ru.porfirevalexey.networkchat.client.gui;

import ru.porfirevalexey.networkchat.connection.Connection;
import ru.porfirevalexey.networkchat.connection.ConnectionListener;
import ru.porfirevalexey.networkchat.message.Message;
import ru.porfirevalexey.networkchat.message.MessageType;
import ru.porfirevalexey.networkchat.message.TextMessage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;


/**
 * Клиентское приложение межсетевого чата с графическим интерфейсом
 */
public class NetworkChatClientGUI extends JFrame implements ActionListener, ConnectionListener {

    private JList usersList;
    private JTextField userMessagetextField;
    private JTextArea chatArea;
    private JLabel userName;
    private JPanel mainPanel;
    private JScrollPane scrollPanel;

    /**
     * Объект соединения с сервером
     */
    private static Connection conn;

    /**
     * Создание нового объекта клиентское приложение межсетевого чата с графическим интерфейсом
     * @param title Заголовок окна
     */
    public NetworkChatClientGUI(String title) {
        super(title);
        try {
            // создаем соединени и подключаемся к серверу
            conn = new Connection(this);
            conn.connectToServer();
        } catch (IOException e) {
            // если сервер не запущен
            JFrame jFrame = new JFrame();
            JOptionPane.showMessageDialog(jFrame, "Не удалось установить соединение с сервером", "Ошибка!", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setContentPane(mainPanel);
        createUIComponents();
        this.pack();
        this.setVisible(true);


        // действия при нажатии на крестик окна - отключаемся от сервера, если соединение активно
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    if (conn.isConnected()) {
                        conn.disconnectFromServer();
                    }
                } catch (IOException ex) {
                    JFrame jFrame = new JFrame();
                    JOptionPane.showMessageDialog(jFrame, "Ошибка отключения от сервера", "Ошибка!", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    /**
     * Инациализация графических компонент
     */
    private void createUIComponents() {
        mainPanel.setPreferredSize(new Dimension(640, 480));
        this.setLocation(500, 500);
        this.setResizable(false);
        this.setLocationRelativeTo(null);
        this.setAlwaysOnTop(true);
        this.userMessagetextField.addActionListener(this);
        this.userName.setText(conn.getUserName());
        this.chatArea.setEditable(false);
    }

    public static void main(String[] args) throws IOException {
        new NetworkChatClientGUI("Client App");
    }

    @Override
    /**
     * Обработка нажатия кнопки Enter при отправке сообщения
     */
    public void actionPerformed(ActionEvent e) {
        String message = this.userMessagetextField.getText();
        if ("".equals(message)) return;
        // формируем новое сообщение и отправляем его на сервер
        Message msg = new TextMessage(conn.getUserName(), message);
        conn.sendMessageToServer(msg);
        // сбрасываем содержимое поля
        this.userMessagetextField.setText(null);
    }

    @Override
    /**
     * Обработка события получения нового текстового сообщения
     * @param message Полученное ткестовое сообщение
     */
    public void newMessageReceived(Message msg) {
        if (msg.getMessageType() == MessageType.TEXT) {
            // добавляем в поле сообщений новое собощение и перемещаем курсор в самый конец
            // это же далем и в других обработчиках
            this.chatArea.append(generateMessageForChat(msg.toString()));
            this.chatArea.setCaretPosition(this.chatArea.getDocument().getLength());
        }
    }

    @Override
    /**
     * Обработка события установки нового соединения
     */
    public void newConnectionEstablished() {
        this.chatArea.append("Соединение с сервером установлено..." + "\n");
        this.chatArea.setCaretPosition(this.chatArea.getDocument().getLength());
        this.chatArea.setCaretPosition(this.chatArea.getDocument().getLength());

    }

    @Override
    /**
     * Обработка события отключения пользователя от сервера
     */
    public void disconnected() {
        this.chatArea.append("Соединение с сервером разорвано..." + "\n");
        this.chatArea.setCaretPosition(this.chatArea.getDocument().getLength());
        //threadReceiver.interrupt();
        System.exit(0);
    }

    @Override
    /**
     * Обработка события добавления нового пользователя на сервер
     * @param userName Новый пользователь
     */
    public void newUserAdded(String userName) {
        chatArea.append(generateMessageForChat(userName + " присоединился к чату"));
        this.chatArea.setCaretPosition(this.chatArea.getDocument().getLength());
        //запрашиваем новый список пользователей и обновляем его в окне списка в приложении
        usersList.setListData(conn.getUsers().toArray());
    }

    @Override
    /**
     * Обработка события удаления пользователя с сервера
     * @param userName Новый пользователь
     */
    public void userWasRemoved(String userName) {
        chatArea.append(generateMessageForChat(userName + " покинул чат"));
        this.chatArea.setCaretPosition(this.chatArea.getDocument().getLength());
        //запрашиваем новый список пользователей и обновляем его в окне списка в приложении
        usersList.setListData(conn.getUsers().toArray());
    }

    @Override
    /**
     * Обработка события успешного изменения имени пользователя
     * @param oldUserName Старое имя пользователя
     * @param newUserName Новое имя пользователя
     */
    public void userNameWasChanged(String oldUserName, String newUserName) {
        chatArea.append(generateMessageForChat(oldUserName + " сменил ник на " + newUserName));
        this.chatArea.setCaretPosition(this.chatArea.getDocument().getLength());
        //запрашиваем новый список пользователей и обновляем его в окне списка в приложении
        usersList.setListData(conn.getUsers().toArray());
        // меняем имя пользователя в окне
        if(this.userName.getText().equals(oldUserName)) {
            this.userName.setText(conn.getUserName());
        }
    }

    @Override
    /**
     * Обработка события неуспешной попытки изменения имени пользователя
     * @param oldUserName Старое имя пользователя
     * @param newUserName Новое имя пользователя
     */
    public void userNameWasNotChanged(String oldUserName, String newUserName) {
        chatArea.append(generateMessageForChat(newUserName + " - такой ник уже существует! Выберите другой ник"));
    }

    @Override
    /**
     * Обработка события получения списка пользователей на сервера
     * @param users Список пользователей на сервере
     */
    public void userListReceived(Set<String> users) {
        usersList.setListData(conn.getUsers().toArray());
    }

    /**
     * Создание текстового представления полученного сообщения для вывода в окне чата в приложении
     * @param message Полученное сообщение
     * @return Строка, содержащая текстовое представление переданного сообщения
     */
    private String generateMessageForChat(String message) {
        return "[" + new Timestamp(System.currentTimeMillis()) + "]" + message + "\n";
    }
}

package ru.porfirevalexey.networkchat.connection;

import ru.porfirevalexey.networkchat.message.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;

public class Connection {

    private final String CONFIG_FILE = "client.cfg";
    private final String LOG_FILE = "logs.txt";

    private SocketChannel socketChannel;
    private ConnectionListener connectionListener;
    private String hostName;
    private int hostPort;
    private int bufferSize;
    private String userName;
    private SortedSet<String> users;
    private boolean isConnected;
    private boolean isCorrectUsername;

    private static BufferedWriter logFile;

    public Connection(ConnectionListener connectionListener) throws IOException {
        this.connectionListener = connectionListener;
        logFile = new BufferedWriter(
                new FileWriter(getClass()
                        .getResource("/").getPath() + LOG_FILE
                )
        );
        users = new TreeSet<>();
        isCorrectUsername = true;
    }

    public void receiveMessageFromServer() {
        final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        int bytesCount = 0;
        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                try {
                    if (socketChannel.isOpen()) {
                        socketChannel.close();
                        log("INFO", "Соединение с сервером было разорвано, т.к. пользователь закончил сеанс", true);
                        isConnected = false;
                    }
                } catch (IOException e) {
                    log("ERROR", "Ошибка при попытке разорвать соединение с сервером", true);
                    isConnected = false;
                }
                break;
            }
            try {
                bytesCount = socketChannel.read(buffer);
                if (bytesCount == -1) {
                    log("INFO", "Сервер завершил сеанс связи", true);
                    this.users.clear();
                    socketChannel.close();
                    break;
                }
                buffer.flip();
                Message message = MessageManager.unpackMessageFromByteArray(buffer.array());
                log("MESSAGE", message.toString(), true);
                buffer.clear();

                if (message.getMessageMode() == MessageMode.STANDARD) {
                    if (message.getMessageType() == MessageType.TEXT) {
                        connectionListener.newMessageReceived(message);
                    }
                } else {
                    String[] contentData = (new String(message.getContent())).split("\\s+");
                    switch (contentData[0]) {
                        case "/exit":
                            log("INFO", "Пользователь покинул " + message.getUsername() + " чат", true);
                            users.remove(message.getUsername());
                            connectionListener.userWasRemoved(message.getUsername());
                            isConnected = false;
                            break;
                        case "/changeName":
                            log("INFO", "Пользователь изменил ник " + message.getUsername() + "->" + contentData[1], true);
                            if (message.getUsername().equals(this.userName) && !isCorrectUsername) {
                                setUserName(contentData[1]);
                                isCorrectUsername = true;
                            }
                            users.add(contentData[1]);
                            connectionListener.userNameWasChanged(message.getUsername(), contentData[1]);
                            break;
                        case "/newUser":
                            log("INFO", "Новый пользователь " + message.getUsername() + " зашел в чат", true);
                            users.add(message.getUsername());
                            connectionListener.newUserAdded(message.getUsername());
                            break;
                        case "/errorInitialUserName":
                            log("INFO", "Пользователь с ником " + contentData[1] + "уже есть", true);
                            setUserName(message.getUsername());
                            connectionListener.userNameWasNotChanged(message.getUsername(), contentData[1]);
                            isCorrectUsername = false;
                        case "/userList":
                            log("INFO", "Получен текущий список участников чата", true);
                            String[] usersFromServer = (new String(message.getContent())).split("\\s+")[1].split(":");
                            users.addAll(Arrays.asList(usersFromServer));
                            connectionListener.userListReceived(users);
                            break;
                        case "/errorUserName":
                            log("INFO", "Не удалось сменить ник пользователя. Пользователь с ником " + contentData[1] + "уже есть", true);
                            setUserName(message.getUsername());
                            connectionListener.userNameWasNotChanged(message.getUsername(), contentData[1]);
                            isCorrectUsername = false;
                            break;
                    }
                }
            } catch (IOException e) {
                log("ERROR", "Ошибка при чтении сообщения от сервера", true);
            } catch (ClassNotFoundException e) {
                log("ERROR", "Ошибка при обработке сообщения от сервера", true);
            }
        }
    }

    public void sendMessageToServer(Message message) {
        try {
            MessageManager.packMessageToByteArray(message);
            final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            buffer.put(MessageManager.packMessageToByteArray(message));
            buffer.flip();
            socketChannel.write(buffer);
            log("INFO", message.toString(), true);

            if (message.getMessageMode() == MessageMode.SERVICE) {
                String[] contentParts = (new String(message.getContent())).split("\\s+");
                switch (contentParts[0]) {
                    case "/exit":
                        log("INFO", "Соединение с сервером разорвано, т.к. пользователь закончил сеанс", true);
                        this.users.clear();
                        connectionListener.userWasRemoved(this.userName);
                        connectionListener.disconnected();
                        socketChannel.close();
                        isConnected = false;
                        break;
                    case "/changeName":
                        log("INFO", "Попытка изменения логина пользователя: " + this.userName + "->" + contentParts[1], true);
                        connectionListener.userNameWasChanged(this.userName, contentParts[1]);
                        setUserName(contentParts[1]);
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void connectToServer() throws IOException {
        readConfigurationFile(CONFIG_FILE);
        InetSocketAddress socketAddress = new InetSocketAddress(hostName, hostPort);
        socketChannel = SocketChannel.open();
        socketChannel.connect(socketAddress);
        connectionListener.newConnectionEstablished();
        log("INFO", "Соединение с сервером установлено", true);
        Message message = new TextMessage(userName, ("/newUser").getBytes(), MessageMode.SERVICE);
        sendMessageToServer(message);
        isConnected = true;
    }

    public void disconnectFromServer() throws IOException {
        Message message = new TextMessage(userName, "/exit".getBytes(), MessageMode.SERVICE);
        sendMessageToServer(message);
        socketChannel.close();
        connectionListener.disconnected();
        log("INFO", "Соединение с сервером разорвано", true);
        this.users.clear();
        isConnected = false;
    }

    private void readConfigurationFile(String fileName) {
        try (BufferedReader br = new BufferedReader(
                new FileReader(getClass().getResource("/" + CONFIG_FILE).getPath())
        )) {
            hostName = br.readLine().strip().split("=")[1];
            hostPort = Integer.parseInt(br.readLine().strip().split("=")[1]);
            bufferSize = Integer.parseInt(br.readLine().strip().split("=")[1]);
            userName = br.readLine().strip().split("=")[1] + (new Random()).nextInt(10);
        } catch (FileNotFoundException e) {
            log("ERROR", "Конфигурационный файл сервера\"" + fileName + "\" не найден", true);
        } catch (IOException e) {
            log("ERROR", "Ошибка чтения конфигурационного файла \"" + fileName + "\"", true);
        }
    }

    private void log(String mode, String msg) {
        log(mode, msg, false);
    }

    private void log(String mode, String msg, boolean writeToFile) {
        String logMessage = String.format("[%s][%s] %s\n",
                new Timestamp(System.currentTimeMillis()),
                mode,
                msg);
        System.out.print(logMessage);
        if (writeToFile) {
            try {
                logFile.write(logMessage);
                logFile.flush();
            } catch (IOException e) {
                log("ERROR", "Ошибка записи в файл логов");
            }
        }
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public SortedSet<String> getUsers() {
        return users;
    }

    public boolean isConnected() {
        return isConnected;
    }
}

package ru.porfirevalexey.networkchat.server;

import ru.porfirevalexey.networkchat.message.Message;
import ru.porfirevalexey.networkchat.message.MessageManager;
import ru.porfirevalexey.networkchat.message.MessageMode;
import ru.porfirevalexey.networkchat.message.TextMessage;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class Server {

    private final String CONFIG_FILE = "server.cfg";
    private final String LOG_FILE = "logs.txt";

    private String host;
    private int port;
    private int bufferSize;
    private BufferedWriter logFile;

    private ServerSocketChannel serverChannel;
    private Selector selector;

    private HashMap<SocketChannel, ByteBuffer> userSockets;
    private HashMap<SocketChannel, String> userNames;
    private HashMap<SocketChannel, Boolean> userCapable;

    private Server() {
        start();
    }

    public static void main(String[] args) throws IOException {
        (new Server()).proceedMessages();
    }

    private void start() {
        try {
            readConfigurationFile(CONFIG_FILE);
            serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(host, port));
            serverChannel.configureBlocking(false);

            selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            logFile = new BufferedWriter(
                    new FileWriter(getClass()
                            .getResource("/" ).getPath() + LOG_FILE
                    )
            );


            log("INFO", "Сервер запущен...", true);
        } catch (IOException e) {
            log("ERROR", "Ошибка запуска сервера", true);
        }
        userSockets = new HashMap<>();
        userNames = new HashMap<>();
        userCapable = new HashMap<>();
    }


    private void proceedMessages() throws IOException {
        try {
            while (true) {
                selector.select();
                for (SelectionKey event : selector.selectedKeys()) {
                    if (event.isValid()) {
                        if (event.isAcceptable()) {
                            acceptNewConnection();
                        } else if (event.isReadable()) {
                            receiveNewMessageFromUser(event);
                        } else if (event.isWritable()) {
                            sendMessageToUser(event);
                        }
                    }
                }
                selector.selectedKeys().clear();
            }
        } catch (IOException e) {
            log("ERROR", "Ошибка сервера при обработке сообщений", true);
        } finally {
            serverChannel.close();
            log("INFO", "Сервер завершил работу", true);
            logFile.close();
        }
    }

    private void sendMessageToUser(SelectionKey event) {
        SocketChannel socketChannel = (SocketChannel) event.channel();
        ByteBuffer buffer = userSockets.get(socketChannel);
        buffer.flip();
        try {
            socketChannel.write(buffer);
            if (!buffer.hasRemaining()) {
                buffer.compact();
                socketChannel.register(selector, SelectionKey.OP_READ);
            }
        } catch (ClosedChannelException e) {
            log("ERROR", "Операция чтения/записи не возможна, так как канал уже закрыт", true);
        } catch (IOException e) {
            log("ERROR", "Ошибка операции записи содержимого буфера в поток", true);
        }
    }

    private void receiveNewMessageFromUser(SelectionKey event) throws IOException {
        SocketChannel socketChannel = null;
        try {
            socketChannel = (SocketChannel) event.channel();
            ByteBuffer buffer = userSockets.get(socketChannel);
            int bytesCount = socketChannel.read(buffer);
            if (bytesCount == -1) {
                log("INFO", "Подключение с адресом " + socketChannel.getRemoteAddress() + " разорвано", true);
                disconnectUser(socketChannel);
                return;
            }

            buffer.flip();
            ByteArrayInputStream byteIs = new ByteArrayInputStream(buffer.array());
            ObjectInputStream objectIs = new ObjectInputStream(byteIs);
            Message message = (Message) objectIs.readObject();
            byteIs.close();
            objectIs.close();

            if (isServiceMessage(message)) {
                proceedServiceMessage(socketChannel, message);
            } else {
                log("MESSAGE", message.toString(), true);
                if (bytesCount > 0 && userCapable.get(socketChannel).equals(Boolean.TRUE)) {
                    broadcast(buffer.array());
                }
            }
        } catch (IOException e) {
            log("ERROR", "Ошибка чтения сообщения от пользователя: " + e.getMessage(), true);
            disconnectUser(socketChannel);
        } catch (ClassNotFoundException e) {
            log("ERROR", "Ошибка обработки сообщения от пользователя" + e.getMessage(), true);
        }
    }

    private void disconnectUser(SocketChannel socketChannel) throws IOException {
        log("INFO", "Пользователь " + userNames.get(socketChannel) + " покинул чат", true);
        userSockets.remove(socketChannel);
        userNames.remove(socketChannel);
        socketChannel.close();
    }

    private void broadcast(byte[] message) throws ClosedChannelException {
        for (Map.Entry<SocketChannel, ByteBuffer> entry : userSockets.entrySet()) {
            entry.getValue().clear();
            entry.getValue().put(ByteBuffer.wrap(message));
            entry.getKey().register(selector, SelectionKey.OP_WRITE);
        }
    }

    private void broadcastExceptOneUser(byte[] message, SocketChannel socketChannel) throws ClosedChannelException {
        for (Map.Entry<SocketChannel, ByteBuffer> entry : userSockets.entrySet()) {
            if (!entry.getKey().equals(socketChannel)) {
                entry.getValue().clear();
                entry.getValue().put(ByteBuffer.wrap(message));
                entry.getKey().register(selector, SelectionKey.OP_WRITE);
            }
        }
    }

    private void acceptNewConnection() {
        try {
            SocketChannel socketChannel = serverChannel.accept();
            socketChannel.configureBlocking(false);
            log("INFO", "Новое подключение с адреса " + socketChannel.getRemoteAddress(), true);
            userSockets.put(socketChannel, ByteBuffer.allocate(bufferSize));
            socketChannel.register(selector, SelectionKey.OP_READ);
        } catch (IOException e) {
            log("ERROR", "Ошибка установки нового подключения", true);
        }
    }

    private void readConfigurationFile(String fileName) {
        try (BufferedReader br = new BufferedReader(
                new FileReader(
                        new File(getClass().getResource("/" + fileName).toURI())
                )
        )) {
            host = br.readLine().strip().split("=")[1];
            port = Integer.parseInt(br.readLine().strip().split("=")[1]);
            bufferSize = Integer.parseInt(br.readLine().strip().split("=")[1]);
        } catch (FileNotFoundException e) {
            log("ERROR", "Конфигурационный файл сервера\"" + fileName + "\" не найден", true);
        } catch (IOException | URISyntaxException e) {
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

    private boolean isServiceMessage(Message message) {
        return MessageMode.SERVICE == message.getMessageMode();
    }

    private void proceedServiceMessage(SocketChannel socketChannel, Message message) throws IOException {
        String[] messageParts = (new String(message.getContent())).split("\\s");
        switch (messageParts[0]) {
            case "/exit":
                final ByteBuffer buffer = userSockets.get(socketChannel);
                broadcastExceptOneUser(buffer.array(), socketChannel);
                disconnectUser(socketChannel);
                break;
            case "/newUser":
                if (!isUserNameCorrect(message.getUsername())) {
                    log("INFO", "Пользователь " + message.getUsername() + " присоединился к чату", true);
                    this.userNames.put(socketChannel, message.getUsername());
                    this.userCapable.put(socketChannel, Boolean.TRUE);
                    this.userSockets.get(socketChannel).flip();
                    broadcastExceptOneUser(this.userSockets.get(socketChannel).array(), socketChannel);
                    Message msg = new TextMessage(
                            message.getUsername(),
                            userNames
                                    .values()
                                    .stream()
                                    .reduce("/userList ", (a, b) -> a + ":" + b).getBytes(),
                            MessageMode.SERVICE);
                    MessageManager.packMessageToByteArray(msg);
                    userSockets.get(socketChannel).clear();
                    userSockets.get(socketChannel).put(ByteBuffer.wrap(MessageManager.packMessageToByteArray(msg)));
                    socketChannel.register(selector, SelectionKey.OP_WRITE);
                } else {
                    this.userCapable.put(socketChannel, Boolean.FALSE);
                    log("INFO", "Пользователь " + message.getUsername() + " не смог присоединиться к чату", true);
                    Message msg = new TextMessage(
                            message.getUsername(),
                            userNames
                                    .values()
                                    .stream()
                                    .reduce("/errorInitialUserName ", (subtotal, element) -> subtotal + ":" + element).getBytes(),
                            MessageMode.SERVICE);
                    final ByteBuffer bufferClient = userSockets.get(socketChannel);
                    bufferClient.clear();
                    bufferClient.put(ByteBuffer.wrap(MessageManager.packMessageToByteArray(msg)));
                    socketChannel.register(selector, SelectionKey.OP_WRITE);
                }
                break;
            case "/changeName":
                if (!isUserNameCorrect(messageParts[1])) {
                    this.userCapable.put(socketChannel, Boolean.TRUE);
                    log("INFO", "Пользователь " + message.getUsername() + " сменил ник на " + messageParts[1], true);
                    this.userNames.put(socketChannel, messageParts[1]);
                    broadcast(userSockets.get(socketChannel).array());
                } else {
                    this.userCapable.put(socketChannel, Boolean.FALSE);
                    log("INFO", "Пользователь " + message.getUsername() + " не смог сменить ник на " + messageParts[1], true);
                    Message msg = new TextMessage(
                            message.getUsername(),
                            ("/errorUserName " + messageParts[1]).getBytes(),
                            MessageMode.SERVICE);
                    final ByteBuffer bufferClient = userSockets.get(socketChannel);
                    bufferClient.clear();
                    bufferClient.put(ByteBuffer.wrap(MessageManager.packMessageToByteArray(msg)));
                    socketChannel.register(selector, SelectionKey.OP_WRITE);
                }
                break;
        }
    }

    private boolean isUserNameCorrect(String userName) {
        return userNames.containsValue(userName);
    }


}

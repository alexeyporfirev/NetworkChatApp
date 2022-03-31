package ru.porfirevalexey.networkchat.server;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class Server {

    private final String CONFIG_FILE = "server.cfg";
    private final String LOG_FILE = "logs.txt";

    private String host;
    private int port;
    private int bufferSize;
    private static BufferedWriter logFile;

    private ServerSocketChannel serverChannel;
    private Selector selector;

    private HashMap<SocketChannel, ByteBuffer> userSockets;
    private HashMap<SocketChannel, String> userNames;

    private Server() {
        start();
    }

    public static void main(String[] args) throws IOException {
        (new Server()).proceedMessages();
        logFile.close();
    }

    private void start() {
        try {
            readConfigurationFile(CONFIG_FILE);
            serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(host, port));
            serverChannel.configureBlocking(false);

            selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

           logFile = new BufferedWriter(new FileWriter(LOG_FILE));

            log("INFO", "Сервер запущен...");
        } catch (IOException e) {
            log("ERROR", "Ошибка запуска сервера");
        } catch (URISyntaxException e) {
            log("ERROR","Неверно указано имя конфигурационного файла при запуске сервера");
        }
        userSockets = new HashMap<>();
    }


    private void proceedMessages() throws IOException {
        try {
            while (true) {
                selector.select();
                for (SelectionKey event : selector.selectedKeys()) {
                    System.out.println(event);
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
            log("ERROR", "Ошибка сервера при обработке сообщений");
        } finally {
            serverChannel.close();
            log("INFO", "Сервер завершил работу");
        }
    }

    private void sendMessageToUser(SelectionKey event) {
        System.out.println("sendMessageToUser:: Start");
        SocketChannel socketChannel = (SocketChannel) event.channel();
        ByteBuffer buffer = userSockets.get(socketChannel);
        buffer.flip();

        try {
            socketChannel.write(buffer);
        } catch (IOException e) {
            log("ERROR", "Ошибка операции записи содержимого буфера в поток");
        }

        if (!buffer.hasRemaining()) {
            buffer.compact();
            try {
                socketChannel.register(selector, SelectionKey.OP_READ);
            } catch (ClosedChannelException e) {
                log("ERROR", "Операция чтения/записи не возможна, так как канал уже закрыт");
            }
        }
        System.out.println("sendMessageToUser:: Done");
    }

    private void receiveNewMessageFromUser(SelectionKey event) throws IOException {
        System.out.println("receiveNewMessageFromUser:: Start");
        SocketChannel socketChannel = null;
        try {
            socketChannel = (SocketChannel) event.channel();
            ByteBuffer buffer = userSockets.get(socketChannel);
            int bytesCount = socketChannel.read(buffer);
            if (bytesCount == -1) {
                log("INFO", "Подключение с адресом " + socketChannel.getRemoteAddress() + " разорвано");
                userSockets.remove(socketChannel);
                socketChannel.close();
            }
            if (bytesCount > 0 && buffer.get(buffer.position() - 1) == '\n') {
                socketChannel.register(selector, SelectionKey.OP_WRITE);
            }
            buffer.flip();
            String userMessage = new String(buffer.array(), buffer.position(), buffer.limit(), StandardCharsets.UTF_8.name());
            log("MESSAGE", userMessage);
            broadcast(userMessage);

            System.out.println("receiveNewMessageFromUser:: Done");
        } catch (IOException e) {
            log("ERROR", "Ошибка чтения сообщения от пользователя");
            userSockets.remove(socketChannel);
            socketChannel.close();
        }
    }

    private void broadcast(String userMessage) throws ClosedChannelException {
        for (Map.Entry<SocketChannel, ByteBuffer> entry : userSockets.entrySet()) {
            entry.getValue().clear();
            entry.getValue().put(ByteBuffer.wrap(userMessage.getBytes()));
            entry.getKey().register(selector, SelectionKey.OP_WRITE);
        }
    }

    private void acceptNewConnection() {
        System.out.println("acceptNewConnection:: Start");
        try {
            SocketChannel socketChannel = serverChannel.accept();
            socketChannel.configureBlocking(false);
            log("INFO", "Новое подключение с адреса " + socketChannel.getRemoteAddress());
            userSockets.put(socketChannel, ByteBuffer.allocate(bufferSize));
            socketChannel.register(selector, SelectionKey.OP_READ);
            System.out.println("acceptNewConnection:: Done");
        } catch (IOException e) {
            log("ERROR", "Ошибка установки нового подключения");
        }
    }

    private void readConfigurationFile(String fileName) throws URISyntaxException {
        File configFile = new File(getClass().getResource("/" + fileName).toURI());
        try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
            host = br.readLine().strip().split("=")[1];
            port = Integer.parseInt(br.readLine().strip().split("=")[1]);
            bufferSize = Integer.parseInt(br.readLine().strip().split("=")[1]);
        } catch (FileNotFoundException e) {
            log("ERROR", "Конфигурационный файл сервера\"" + fileName + "\" не найден");
        } catch (IOException e) {
            log("ERROR", "Ошибка чтения конфигурационного файла \"" + fileName + "\"");
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
            } catch (IOException e) {
                log("ERROR", "Ошибка записи в файл логов");
            }
        }
    }
}

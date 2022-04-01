package ru.porfirevalexey.networkchat.connection;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Scanner;

public class Connection {

    private final String CONFIG_FILE = "client.cfg";
    private final String LOG_FILE = "client.cfg";

    private SocketChannel socketChannel;
    private ConnectionListener connectionListener;
    private String hostName;
    private int hostPort;
    private int bufferSize;
    private String userName;

    private static BufferedWriter logFile;

    public Connection(ConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    public void receiveMessageFromServer() {
        final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        int bytesCount = 0;
        while (true && !Thread.currentThread().isInterrupted()) {
            try {
                bytesCount = socketChannel.read(buffer);
                buffer.flip();
                String msg = new String(buffer.array(), buffer.position(), bytesCount, StandardCharsets.UTF_8).trim();
                System.out.println("Result: " + msg);
                buffer.clear();
                connectionListener.newMessageReceived(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

//    public void sendMessageToServer() {
//        try {
//            Scanner scanner = new Scanner(System.in);
//            final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
//            String msg = null;
//            while (true) {
//                msg = scanner.nextLine().trim() + "\r\n";
//                if ("\\exit\r\n".equals(msg)) {
//                    break;
//                }
//                socketChannel.write(ByteBuffer.wrap(
//                        msg.getBytes(StandardCharsets.UTF_8)));
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public void sendMessageToServer(String message) {
        try {
            final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            message = message.trim() + "\r\n";
            if ("\\exit\r\n".equals(message)) {
                return;
            }
            socketChannel.write(ByteBuffer.wrap(
                    message.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void connectToServer() throws IOException {
        readConfigurationFile(CONFIG_FILE);
        InetSocketAddress socketAddress = new InetSocketAddress(hostName, hostPort);
        socketChannel = SocketChannel.open();
        socketChannel.connect(socketAddress);
    }

    public void disconnectFromServer() throws IOException {
        socketChannel.close();
    }
    private void readConfigurationFile(String fileName) throws IOException {
        try (BufferedReader bf = new BufferedReader(
                new FileReader(
                        new File(
                                getClass()
                                        .getResource("/" + fileName)
                                        .toURI()
                        )
                )
        )) {
            while (bf.ready()) {
                hostName = bf.readLine().strip().split("=")[1];
                hostPort = Integer.parseInt(bf.readLine().strip().split("=")[1]);
                bufferSize = Integer.parseInt(bf.readLine().strip().split("=")[1]);
                userName = bf.readLine().strip().split("=")[1];
            }
        } catch (URISyntaxException | FileNotFoundException e) {
            log("ERROR", "Конфигурационный файл не найден");
        } catch (IOException e) {
            log("ERROR", "Ошибка чтения конфигурационного файла");
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

    public String getUserName() {
        return userName;
    }
}

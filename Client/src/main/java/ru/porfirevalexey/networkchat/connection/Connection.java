package ru.porfirevalexey.networkchat.connection;

import ru.porfirevalexey.networkchat.message.Message;
import ru.porfirevalexey.networkchat.message.MessageMode;
import ru.porfirevalexey.networkchat.message.TextMessage;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Random;
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
        while (true) {
            if (Thread.currentThread().isInterrupted()) {
                System.out.println("Interrrerererer");
                try {
                    socketChannel.close();
                    System.out.println("11111111");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
            try {
                bytesCount = socketChannel.read(buffer);
                buffer.flip();
                String msg = new String(buffer.array(), buffer.position(), bytesCount, StandardCharsets.UTF_8).trim();
                ByteArrayInputStream byteIs = new ByteArrayInputStream(buffer.array());
                ObjectInputStream objectIs = new ObjectInputStream(byteIs);
                Message message = (Message) objectIs.readObject();
                System.out.println("Result: " + new String(message.getContent()));
                buffer.clear();
                connectionListener.newMessageReceived(message);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessageToServer(Message message) {
        try {
            ByteArrayOutputStream byteOs = new ByteArrayOutputStream();
            ObjectOutputStream objectOs = new ObjectOutputStream(byteOs);
            objectOs.writeObject(message);
            objectOs.flush();

            final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            buffer.put(byteOs.toByteArray());

            byteOs.close();
            objectOs.close();

            buffer.flip();
            socketChannel.write(buffer);

            if (message.getMessageMode() == MessageMode.SERVICE) {
                String[] contentParts = (new String(message.getContent())).split("\\s+");
                switch (contentParts[0]) {
                    case "\\exit":
                        System.out.println("Disconnect from server");
                        connectionListener.disconnected();
                        Thread.sleep(1000);
                        socketChannel.close();
                        break;
                    case "\\changeName":
                        System.out.println();
                        setUserName(contentParts[1]);
                        break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void connectToServer() throws IOException {
        readConfigurationFile(CONFIG_FILE);
        InetSocketAddress socketAddress = new InetSocketAddress(hostName, hostPort);
        socketChannel = SocketChannel.open();
        socketChannel.connect(socketAddress);
        connectionListener.newConnectionEstablished();
        Message message = new TextMessage(userName, ("\\newUser").getBytes(), MessageMode.SERVICE);
        sendMessageToServer(message);
    }

    public void disconnectFromServer() throws IOException {
        Message message = new TextMessage(userName, "\\exit".getBytes(), MessageMode.SERVICE);
        sendMessageToServer(message);
        System.out.println("Exit отправлен");
        socketChannel.close();
        connectionListener.disconnected();
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
                userName = bf.readLine().strip().split("=")[1] + (new Random()).nextInt(10);
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

    public void setUserName(String userName) {
        this.userName = userName;
    }
}

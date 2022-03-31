package ru.porfirevalexey.networkchat.client;

import ru.porfirevalexey.networkchat.connection.Connection;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.channels.SocketChannel;
import java.sql.Timestamp;

public class Client {

    private final String CONFIG_FILE = "client.cfg";
    private final String LOG_FILE = "client.cfg";

    private String hostName;
    private int hostPort;
    private int bufferSize;

    private static BufferedWriter logFile;

    private Client() throws IOException, InterruptedException {
        start();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        new Client();
    }

    private void start() throws IOException, InterruptedException {
        logFile = new BufferedWriter(new FileWriter(LOG_FILE));
        readConfigurationFile(CONFIG_FILE);

        InetSocketAddress socketAddress = new InetSocketAddress(hostName, hostPort);
        final SocketChannel socketChannel = SocketChannel.open();
        socketChannel.connect(socketAddress);

        Connection conn = new Connection(socketChannel, bufferSize);

        Thread rec = new Thread(null, conn::receiveMessageFromServer, "receiveMessageThread");
        Thread send = new Thread(null, conn::sendMessageToServer, "sendMessageThread");

        rec.start();
        send.start();

        send.join();
        rec.interrupt();
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
}

package ru.porfirevalexey.networkchat.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Connection {

    private SocketChannel socketChannel;
    private int bufferSize;

    public Connection(SocketChannel socketChannel, int bufferSize) {
        this.socketChannel = socketChannel;
        this.bufferSize = bufferSize;
    }

    public void receiveMessageFromServer() {
        final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        int bytesCount = 0;
        while (true && !Thread.currentThread().isInterrupted()) {
            try {
                bytesCount = socketChannel.read(buffer);
                buffer.flip();
                System.out.println("Result: " +
                        new String(buffer.array(), buffer.position(), bytesCount, StandardCharsets.UTF_8).trim());
                buffer.clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessageToServer() {
        try {
            Scanner scanner = new Scanner(System.in);
            final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            String msg = null;
            while (true) {
                msg = scanner.nextLine().trim() + "\r\n";
                if ("\\exit\r\n".equals(msg)) {
                    break;
                }
                socketChannel.write(ByteBuffer.wrap(
                        msg.getBytes(StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

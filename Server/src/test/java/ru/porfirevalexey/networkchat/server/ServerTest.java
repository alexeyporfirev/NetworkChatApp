package ru.porfirevalexey.networkchat.server;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;

public class ServerTest {

    private static Server server;
    private static SocketChannel testSocketChannel;

    @BeforeAll
    public static void init() throws IOException {
        server = new Server();
        testSocketChannel = SocketChannel.open();
        server.userNames.put(testSocketChannel, "user1");
        server.userSockets.put(testSocketChannel, ByteBuffer.allocate(1024));
        server.userCapable.put(testSocketChannel, Boolean.TRUE);
    }

    @Test
    public void testUserDisconnectedFromServer() throws IOException {
        server.disconnectUser(testSocketChannel);
        Assertions.assertEquals(0, server.userCapable.size());
        Assertions.assertEquals(0, server.userNames.size());
        Assertions.assertEquals(0, server.userSockets.size());
    }

}

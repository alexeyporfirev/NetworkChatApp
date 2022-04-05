package ru.porfirevalexey.networkchat.connection;

import org.junit.jupiter.api.*;
import ru.porfirevalexey.networkchat.client.gui.NetworkChatClientGUI;
import ru.porfirevalexey.networkchat.message.TextMessage;

import java.io.IOException;

public class ConnectionTest {

    private static Connection conn;

    @BeforeEach
    public void init() {
        System.out.println("Test started!");
    }

    @BeforeAll
    public static void started() {
        try {
            //mock
            conn = new Connection(new NetworkChatClientGUI("test"));
            conn.connectToServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Tests started!");
    }

    @AfterEach
    public void finished() {
        System.out.println("Test completed!");
    }

    @AfterAll
    public static void finishedAll() {
        System.out.println("Tests completed!");
    }

    @Test
    public void testConnectionToActiveServer() throws IOException {
        Assertions.assertEquals(true, conn.isConnected());
    }

    @Test
    public void testSendMessageToServerWithoutExceptions(){
        Assertions.assertDoesNotThrow(()->conn.sendMessageToServer(new TextMessage("user", "message")));
    }

    @Test
    public void testGetUsersFromServer(){
        System.out.println(conn.getUsers());
        Assertions.assertNotEquals(0, conn.getUsers().size());

    }

    @Test
    public void testSetUserName(){
        //arrange
        String result = "123456";
        //result
        conn.setUserName("123456");
        String name = conn.getUserName();

        Assertions.assertEquals(name, result);
    }






}

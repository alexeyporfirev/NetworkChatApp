package ru.porfirevalexey.networkchat.message;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

public class MessageManagerTest {


    @BeforeEach
    public void init() {
        System.out.println("Test started!");
    }

    @BeforeAll
    public static void started() {
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
    public void testPackUnpackServiceMessage() throws IOException, ClassNotFoundException {
        //arrange
        Message message = new TextMessage("user", "/changeName new");
        //act
        Message result = MessageManager.unpackMessageFromByteArray(
                MessageManager.packMessageToByteArray(message));
        //assert
        Assertions.assertEquals(message, result);
    }

    @Test
    public void testPackUnpackStandardMessage() throws IOException, ClassNotFoundException {
        //arrange
        Message message = new TextMessage("user", "hello!");
        //act
        Message result = MessageManager.unpackMessageFromByteArray(
                MessageManager.packMessageToByteArray(message));
        //assert
        Assertions.assertEquals(message, result);
    }



}

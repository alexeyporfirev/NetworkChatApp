package ru.porfirevalexey.networkchat.message;


import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

public class TextMessageTest {

    private TextMessage tMessage;

    @BeforeEach
    public void init() {
        System.out.println("Test started!");
        tMessage = new TextMessage("user", "message");
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

    @ParameterizedTest
    @ValueSource(strings = {"/exit",
            "/newUser user",
            "/newUser 1234",
            "/changeName user",
            "/changeName 1234"
    })
    public void testDefineServiceMessage(String message) {
        TextMessage msg = new TextMessage("user", message);
        Assertions.assertEquals(MessageMode.SERVICE, msg.getMessageMode());

//        //arrange
//        MessageMode expected = MessageMode.SERVICE;
//        //act
//        MessageMode result = sut.pay(20000);
//        //assert
//        assertEquals(expected, result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"hello!",
            "123 asd",
            "привет, ребята",
            ":)",
            "\\nline"
    })
    public void testDefineNonServiceMessage(String message) {
        TextMessage msg = new TextMessage("user", message);
        Assertions.assertEquals(MessageMode.STANDARD, msg.getMessageMode());

//        //arrange
//        MessageMode expected = MessageMode.SERVICE;
//        //act
//        MessageMode result = sut.pay(20000);
//        //assert
//        assertEquals(expected, result);
    }

}

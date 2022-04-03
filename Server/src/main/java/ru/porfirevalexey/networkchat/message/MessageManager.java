package ru.porfirevalexey.networkchat.message;

import java.io.*;

public class MessageManager {

    public static byte[] packMessageToByteArray(Message message) throws IOException {
        ByteArrayOutputStream byteOs = new ByteArrayOutputStream();
        ObjectOutputStream objectOs = new ObjectOutputStream(byteOs);
        objectOs.writeObject(message);
        objectOs.flush();
        byteOs.close();
        objectOs.close();
        return byteOs.toByteArray();
    }

    public static Message unpackMessageFromByteArray(byte[] array) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteIs = new ByteArrayInputStream(array);
        ObjectInputStream objectIs = new ObjectInputStream(byteIs);
        Message message = (Message) objectIs.readObject();
        byteIs.close();
        objectIs.close();
        return message;
    }
}

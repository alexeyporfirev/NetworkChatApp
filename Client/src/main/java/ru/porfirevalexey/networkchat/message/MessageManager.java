package ru.porfirevalexey.networkchat.message;

import java.io.*;

/**
 * Класс для работы с сообщениями
 */
public class MessageManager {

    /**
     * Упаковать объект типа {@link ru.porfirevalexey.networkchat.message.Message} в байтовый массив через сериализацию
     * @param message Исходное сообщение
     * @return Байтовый массив, содержащий сериализованное сообщение
     * @throws IOException В случае ошибки сериализации сообщения в байтовый массив
     */
    public static byte[] packMessageToByteArray(Message message) throws IOException {
        ByteArrayOutputStream byteOs = new ByteArrayOutputStream();
        ObjectOutputStream objectOs = new ObjectOutputStream(byteOs);
        objectOs.writeObject(message);
        objectOs.flush();
        byteOs.close();
        objectOs.close();
        return byteOs.toByteArray();
    }

    /**
     * Получить объект типа {@link ru.porfirevalexey.networkchat.message.Message} из байтового массива через десериализацию
     * @param array Байтовый массив, содержащий сериализованное сообщение
     * @return Объект сообщения
     * @throws IOException В случае ошибки десериализации сообщения в байтовый массив
     * @throws ClassNotFoundException В случае ошибки по созданию объекта класса {@link ru.porfirevalexey.networkchat.message.Message}
     */
    public static Message unpackMessageFromByteArray(byte[] array) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteIs = new ByteArrayInputStream(array);
        ObjectInputStream objectIs = new ObjectInputStream(byteIs);
        Message message = (Message) objectIs.readObject();
        byteIs.close();
        objectIs.close();
        return message;
    }
}

package ru.porfirevalexey.networkchat.message;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * Класс текстовых сообщений
 */
public class TextMessage implements Message, Serializable {

    /**
     * Имя пользователя
     */
    private String username;
    /**
     * Тип сообщения
     */
    private MessageType messageType;
    /**
     * Вид сообщения
     */
    private MessageMode messageMode;
    /**
     * Содержимоесообщения
     */
    private byte[] content;

    /**
     * Создание объекта сообщения с указанием вида сообщения
     * @param username имя пользователя
     * @param content содержимое сообщения
     * @param messageMode вид сообщения
     */
    public TextMessage(String username, byte[] content, MessageMode messageMode) {
        this.username = username;
        this.content = content;
        this.messageMode = messageMode;
        this.messageType = MessageType.TEXT;
    }

    /**
     * Создание объекта сообщения с автоматиеским определением вида сообщения по строковому представлению его содержимого
     * @param username имя пользователя
     * @param message Строковое представление сообщения
     */
    public TextMessage(String username, String message) {
        this.username = username;
        this.messageMode = defineMessageMode(message);
        content = message.getBytes();
        this.messageType = MessageType.TEXT;
    }

    @Override
    /**
     * Получения содержимого сообщения
     */
    public byte[] getContent() {
        return content;
    }

    @Override
    /**
     * Получение имени пользователя
     */
    public String getUsername() {
        return username;
    }

    @Override
    /**
     * Получение вида сообщения
     */
    public MessageMode getMessageMode() {
        return messageMode;
    }

    @Override
    /**
     * Получение типа сообщения
     */
    public MessageType getMessageType() {
        return messageType;
    }

    /**
     * Определение вида сообщения по строковому представлению его содержимого
     * @param message Строковое представление содержимого сообщения
     * @return Вид переданного сообщения
     */
    private MessageMode defineMessageMode(String message) {
        return message.startsWith("/") ? MessageMode.SERVICE : MessageMode.STANDARD;
    }

    @Override
    /**
     * Получение строкового предствления сообщения
     * @return строковое предствление сообщения
     */
    public String toString() {
        return this.username + ": " + new String(content);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TextMessage that = (TextMessage) o;
        return Objects.equals(username, that.username) && messageType == that.messageType && messageMode == that.messageMode && Arrays.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(username, messageType, messageMode);
        result = 31 * result + Arrays.hashCode(content);
        return result;
    }
}

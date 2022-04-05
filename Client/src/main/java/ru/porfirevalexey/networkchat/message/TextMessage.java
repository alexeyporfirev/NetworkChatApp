package ru.porfirevalexey.networkchat.message;

import java.io.Serializable;

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
}

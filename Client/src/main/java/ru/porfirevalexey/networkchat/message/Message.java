package ru.porfirevalexey.networkchat.message;

/**
 * Интерфейс, описывающий базовый функционал собощений
 */
public interface Message {

    /**
     * Получение содержания сообщения в байтовом виде
     * @return Массив с байтовым представлением содержимого сообщения
     */
    byte[] getContent();

    /**
     * Получение имени пользователя, создавшего сообщение
     * @return Имя пользователя
     */
    String getUsername();

    /**
     * Получения вида сообщения
     * @return Вид сообщения
     */
    MessageMode getMessageMode();

    /**
     * Получение типа сообщения
     * @return Тип сообщения
     */
    MessageType getMessageType();
}

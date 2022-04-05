package ru.porfirevalexey.networkchat.connection;

import ru.porfirevalexey.networkchat.message.Message;

import java.util.Set;

/**
 * Интерфейс, описывающий объект-слушатель соединения
 */
public interface ConnectionListener {

    /**
     * Событие получения нового текстового сообщения
     * @param message Полученное ткестовое сообщение
     */
    void newMessageReceived(Message message);

    /**
     * Событие установки нового соединения
     */
    void newConnectionEstablished();

    /**
     * Событие отключения пользователя от сервера
     */
    void disconnected();

    /**
     * Событие добавления нового пользователя на сервер
     * @param userName Новый пользователь
     */
    void newUserAdded(String userName);

    /**
     * Событие удаления пользователя с сервера
     * @param userName Новый пользователь
     */
    void userWasRemoved(String userName);

    /**
     * Событие успешного изменения имени пользователя
     * @param oldUserName Старое имя пользователя
     * @param newUserName Новое имя пользователя
     */
    void userNameWasChanged(String oldUserName, String newUserName);

    /**
     * Событие неуспешной попытки изменения имени пользователя
     * @param oldUserName Старое имя пользователя
     * @param newUserName Новое имя пользователя
     */
    void userNameWasNotChanged(String oldUserName, String newUserName);

    /**
     * Событие получения списка пользователей на сервера
     * @param users Список пользователей на сервере
     */
    void userListReceived(Set<String> users);

}

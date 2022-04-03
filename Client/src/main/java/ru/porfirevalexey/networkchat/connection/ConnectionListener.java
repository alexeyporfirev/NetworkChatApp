package ru.porfirevalexey.networkchat.connection;

import ru.porfirevalexey.networkchat.message.Message;

import java.util.Set;

public interface ConnectionListener {

    void newMessageReceived(Message message);

    void newConnectionEstablished();

    void disconnected();

    void newUserAdded(String userName);

    void userWasRemoved(String userName);

    void userNameWasChanged(String oldUserName, String newUserName);

    void userNameWasNotChanged(String oldUserName, String newUserName);

    void userListReceived(Set<String> users);

}

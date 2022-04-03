package ru.porfirevalexey.networkchat.connection;

import ru.porfirevalexey.networkchat.message.Message;

public interface ConnectionListener {

    void newMessageReceived(Message message);

    void newConnectionEstablished();

    void disconnected();

    //void newUserAdded(String userName);


}

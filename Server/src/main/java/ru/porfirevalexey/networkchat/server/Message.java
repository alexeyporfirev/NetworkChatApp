package ru.porfirevalexey.networkchat.server;

public interface Message {

    byte[] getContent();

    String getUserName();

    String getMessageMode();
}

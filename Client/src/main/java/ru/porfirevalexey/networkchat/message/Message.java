package ru.porfirevalexey.networkchat.message;

public interface Message{

    byte[] getContent();

    String getUsername();

    MessageMode getMessageMode();

    MessageType getMessageType();
}

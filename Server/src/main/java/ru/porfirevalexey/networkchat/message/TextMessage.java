package ru.porfirevalexey.networkchat.message;

import java.io.Serializable;

public class TextMessage implements Message, Serializable {

    private String username;
    private MessageType messageType;
    private MessageMode messageMode;
    private byte[] content;

    public TextMessage(String username, byte[] content, MessageMode messageMode) {
        this.username = username;
        this.content = content;
        this.messageMode = messageMode;
        this.messageType = MessageType.TEXT;
    }

    public TextMessage(String username, String message) {
        this.username = username;
        this.messageMode = defineMessageMode(message);
        content = message.getBytes();
        this.messageType = MessageType.TEXT;
    }

    @Override
    public byte[] getContent() {
        return content;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public MessageMode getMessageMode() {
        return messageMode;
    }

    @Override
    public MessageType getMessageType() {
        return messageType;
    }

    private MessageMode defineMessageMode(String message) {
        return message.startsWith("/") ? MessageMode.SERVICE : MessageMode.STANDARD;
    }

    @Override
    public String toString() {
        return this.username + ": " + new String(content);
    }
}

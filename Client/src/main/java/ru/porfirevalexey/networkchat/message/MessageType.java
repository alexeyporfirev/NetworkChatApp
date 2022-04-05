package ru.porfirevalexey.networkchat.message;

/**
 * Типы сообщений, которые могут быть использованы при создании сообщений
 */
public enum MessageType {
    /**
     * Текстовое сообщение
     */
    TEXT,
    /**
     * Аудио-сообщение
     */
    AUDIO,
    /**
     * Видеосообщение
     */
    VIDEO,
    /**
     * Рисунок
     */
    IMAGE;
}

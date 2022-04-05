package ru.porfirevalexey.networkchat.connection;

import ru.porfirevalexey.networkchat.message.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.sql.Timestamp;
import java.util.*;

/**
 * Класс для работы с соединением с сервером
 */
public class Connection {

    private final String CONFIG_FILE = "client.cfg";
    private final String LOG_FILE = "logs.txt";

    private SocketChannel socketChannel;
    private ConnectionListener connectionListener;
    private String hostName;
    private int hostPort;
    private int bufferSize;
    private String userName;

    // пользователи на сервере
    private SortedSet<String> users;
    // подключен в данный момент или нет
    private boolean isConnected;
    // не дублирует ли ямя текущего пользователя чье-то другое имя
    private boolean isCorrectUsername;

    /**
     * Поток-слушатель входящих сообщений от сервера
     */
    private Thread threadReceiver;

    private static BufferedWriter logFile;

    /**
     * Создание нового подключения к серверу
     * @param connectionListener Объект слушателя подкюлчения
     * @throws IOException В случае ошибки по созданию/открытию файла логов
     */
    public Connection(ConnectionListener connectionListener) throws IOException {
        this.connectionListener = connectionListener;
        logFile = new BufferedWriter(
                new FileWriter(getClass()
                        .getResource("/").getPath() + LOG_FILE
                )
        );
        users = new TreeSet<>();
        isCorrectUsername = true;
    }

    /**
     * Получение нового сообщения от сервера
     */
    public void receiveMessageFromServer() {
        final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        int bytesCount = 0;
        // слушаем, пока не прервут
        while (true) {
            //если поток прервали, то отключаемся от сервера и переходим в состояние "неподключен" к серверу
            if (Thread.currentThread().isInterrupted()) {
                try {
                    disconnectFromServer();
                } catch (IOException e) {
                    log("ERROR", "Ошибка при попытке разорвать соединение с сервером", true);
                    isConnected = false;
                }
                break;
            }
            try {
                // читаем данные из буффера, если вернут -1, то очиащем список пользователей и закрываем канал сокета
                bytesCount = socketChannel.read(buffer);
                if (bytesCount == -1) {
                    log("INFO", "Сервер завершил сеанс связи", true);
                    this.users.clear();
                    socketChannel.close();
                    break;
                }
                // переходим в начало буффера и извлекаем сообщение, после чего чистим буффер
                buffer.flip();
                Message message = MessageManager.unpackMessageFromByteArray(buffer.array());
                log("MESSAGE", message.toString(), true);
                buffer.clear();

                // если стандартное текстовое сообщение, то сообщаем слушателю, что получили текстовое сообщение,
                // иначе обрабатываем его как одно из сервисных сообщений
                if (message.getMessageMode() == MessageMode.STANDARD) {
                    if (message.getMessageType() == MessageType.TEXT) {
                        connectionListener.newMessageReceived(message);
                    }
                } else {
                    // разбиваем сервисное сообщение на две части - метка сервисного сообщение (/exit, /newUser и т.д.) и содержимое
                    // этого сервисного сообщения, которое несет полезную информацию
                    String[] contentData = (new String(message.getContent())).split("\\s+");
                    switch (contentData[0]) {
                        // если это выход какого-то пользователя, то удаляем его из списка пользователей и сообщаем об этом слушателю
                        case "/exit":
                            log("INFO", "Пользователь покинул " + message.getUsername() + " чат", true);
                            users.remove(message.getUsername());
                            connectionListener.userWasRemoved(message.getUsername());
                            //isConnected = false;
                            break;
                        // если это смена имени  пользователя текущего соединения
                        case "/changeName":
                            log("INFO", "Пользователь изменил ник " + message.getUsername() + "->" + contentData[1], true);
                            // если имя совпадает с именем пользователя текущего соединения и у него была установлена метка
                            // некорретного имени, то меняем имя на новое, добавляем его в список и убираем метку
                            // если имя совпадает с именем пользователя текущего соединения, но у него не была установлена метка
                            // некорретного имени, то меняем имя на новое, удаляем его старое имя из списка и добавляем в список новое
                            // в остальных случаях просто удаляем старое имя из списка и добавлем новое
                            if (message.getUsername().equals(this.userName) && !isCorrectUsername) {
                                setUserName(contentData[1]);
                                isCorrectUsername = true;
                                users.add(contentData[1]);
                            } else if (message.getUsername().equals(this.userName) && isCorrectUsername){
                                users.remove(message.getUsername());
                                users.add(contentData[1]);
                                setUserName(contentData[1]);
                            } else {
                                users.remove(message.getUsername());
                                users.add(contentData[1]);
                            }
                            //сообщаем слушателю об изменении имени пользователя
                            connectionListener.userNameWasChanged(message.getUsername(), contentData[1]);
                            break;
                        // если это добавление нового пользователя
                        case "/newUser":
                            log("INFO", "Новый пользователь " + message.getUsername() + " зашел в чат", true);
                            // добавляем его в список и сообщаем об этом слушателю
                            users.add(message.getUsername());
                            connectionListener.newUserAdded(message.getUsername());
                            break;
                        // если это ошибка стартового имени - т.е. при запуске соединения было установлено имя, которое уже было на сервере
                        case "/errorInitialUserName":
                            log("INFO", "Пользователь с ником " + contentData[1] + "уже есть", true);
                            // ничего не делаем и сообщаем, что пользователь с таким именем уже есть
                            connectionListener.userNameWasNotChanged(message.getUsername(), contentData[1]);
                            isCorrectUsername = false;
                            // если это получения списка плоьзователей, то просто его парсим и передаем в качестве нового списка пользователй
                        case "/userList":
                            log("INFO", "Получен текущий список участников чата", true);
                            String[] usersFromServer = (new String(message.getContent())).split("\\s+")[1].split(":");
                            users.addAll(Arrays.asList(usersFromServer));
                            connectionListener.userListReceived(users);
                            break;
                        // если это ошибка смены имени, т.е. новое имя уже занято на сервере, то возвращаем старое имя и сообщаем
                        // об этом слушателю
                        case "/errorUserName":
                            log("INFO", "Не удалось сменить ник пользователя. Пользователь с ником " + contentData[1] + "уже есть", true);
                            setUserName(message.getUsername());
                            connectionListener.userNameWasNotChanged(message.getUsername(), contentData[1]);
                            break;
                            // если это сообщение о смене имени пользователем, который уже был на сервере, то, если это
                        // имя не совпадает с именем пользователя текущего соединения, то удаляем старое имя из списка и
                        // добавляем туда новое имя и сообщаем слушателю об изменении имени пользователя. Инача просто
                        // добавляем новое имя в список и сообщаем об этом пользователю
                        case "/oldUserChangeName":
                            log("INFO", "Пользователь изменил ник " + message.getUsername() + "->" + contentData[1], true);
                            if (!this.userName.equals(message.getUsername())) {
                                users.remove(message.getUsername());
                                users.add(contentData[1]);
                                connectionListener.userNameWasChanged(message.getUsername(), contentData[1]);
                            } else {
                                users.add(contentData[1]);
                                connectionListener.newUserAdded(contentData[1]);
                            }
                            break;
                    }
                }
            } catch (IOException e) {
                log("ERROR", "Ошибка при чтении сообщения от сервера", true);
            } catch (ClassNotFoundException e) {
                log("ERROR", "Ошибка при обработке сообщения от сервера", true);
            }
        }
    }

    /**
     * Отправка сообщение серверу
     * @param message Объект отправляемого сообщения
     */
    public void sendMessageToServer(Message message) {
        try {
            // упаковываем сообщение
            MessageManager.packMessageToByteArray(message);
            final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            buffer.put(MessageManager.packMessageToByteArray(message));
            buffer.flip();
            socketChannel.write(buffer);

            // если сообщение сервисное, то делаем дополнительные действия с текущим подключением
            if (message.getMessageMode() == MessageMode.SERVICE) {
                String[] contentParts = (new String(message.getContent())).split("\\s+");
                switch (contentParts[0]) {
                    // если это было сообщение о выходе текущего пользователя, то очищаем список польователей, закрываем соединение
                    // и передем соответствующую информацию слушателю
                    case "/exit":
                        log("INFO", "Соединение с сервером разорвано, т.к. пользователь закончил сеанс", true);
                        this.users.clear();
                        connectionListener.userWasRemoved(this.userName);
                        connectionListener.disconnected();
                        // прерываем поток, принимающий собощения от сервера
                        threadReceiver.interrupt();
                        socketChannel.close();
                        isConnected = false;
                        break;
                        // если это попытка смены имени пользователя, то просто записываем это в логи
                    case "/changeName":
                        log("INFO", "Попытка изменения логина пользователя: " + this.userName + "->" + contentParts[1], true);
                        break;
                }
            }
        } catch (IOException e) {
            log("ERROR", "Ошибка отправки сообщения: " + message, true);
        }
    }

    /**
     * Устанавливаем соединение с сервером
     * @throws IOException В случае ошибки по созданию канала сокета или при попытке соединиться с сервером
     */
    public void connectToServer() throws IOException {
        readConfigurationFile(CONFIG_FILE);
        InetSocketAddress socketAddress = new InetSocketAddress(hostName, hostPort);
        socketChannel = SocketChannel.open();
        socketChannel.connect(socketAddress);
        connectionListener.newConnectionEstablished();
        log("INFO", "Соединение с сервером установлено", true);
        // отправляем сообщение о новом пользователе
        Message message = new TextMessage(userName, ("/newUser").getBytes(), MessageMode.SERVICE);
        sendMessageToServer(message);
        isConnected = true;
        Thread threadReceiver = new Thread(null, this::receiveMessageFromServer, "receiveMessages");
        threadReceiver.start();
    }

    /**
     * Отключаемся от сервера
     * @throws IOException В случае ошибки закрытия канала сокета
     */
    public void disconnectFromServer() throws IOException {
        // отправляем сообщение о выходе пользователя
        Message message = new TextMessage(userName, "/exit".getBytes(), MessageMode.SERVICE);
        sendMessageToServer(message);
        socketChannel.close();
        connectionListener.disconnected();
        log("INFO", "Соединение с сервером разорвано", true);
        this.users.clear();
        isConnected = false;
    }

    /**
     * Чтение конфигурационного файла
     * @param fileName Имя конфигурационного файла
     */
    private void readConfigurationFile(String fileName) {
        try (BufferedReader br = new BufferedReader(
                new FileReader(getClass().getResource("/" + CONFIG_FILE).getPath())
        )) {
            hostName = br.readLine().strip().split("=")[1];
            hostPort = Integer.parseInt(br.readLine().strip().split("=")[1]);
            bufferSize = Integer.parseInt(br.readLine().strip().split("=")[1]);
            userName = br.readLine().strip().split("=")[1] + (new Random()).nextInt(2);
        } catch (FileNotFoundException e) {
            log("ERROR", "Конфигурационный файл сервера\"" + fileName + "\" не найден", true);
        } catch (IOException e) {
            log("ERROR", "Ошибка чтения конфигурационного файла \"" + fileName + "\"", true);
        }
    }

    /**
     * Запись лога в консоль
     * @param mode Вид логируемого сообщения
     * @param msg Логируемое собощение
     */
    private void log(String mode, String msg) {
        log(mode, msg, false);
    }

    /**
     * Запись лога в консоль с возможность дополнительной записи в файл логов
     * @param mode Вид логируемого сообщения
     * @param msg Логируемое собощение
     * @param writeToFile Надо ли записывать логи в файл логов
     */
    private void log(String mode, String msg, boolean writeToFile) {
        String logMessage = String.format("[%s][%s] %s\n",
                new Timestamp(System.currentTimeMillis()),
                mode,
                msg);
        System.out.print(logMessage);
        if (writeToFile) {
            try {
                logFile.write(logMessage);
                logFile.flush();
            } catch (IOException e) {
                log("ERROR", "Ошибка записи в файл логов");
            }
        }
    }

    /**
     * Получение имени пользователя текущего соединения
     * @return Имя пользователя
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Изменение имени пользователя текущего соединения
     * @param userName Новое имя пользователя
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Получение текущего списка пользователей
     * @return Список пользователй
     */
    public SortedSet<String> getUsers() {
        return users;
    }

    /**
     * Проверка активно соединение или нет
     * @return true - если соединение активно, иначе - false
     */
    public boolean isConnected() {
        return isConnected;
    }

}

package ru.porfirevalexey.networkchat.server;

import ru.porfirevalexey.networkchat.message.Message;
import ru.porfirevalexey.networkchat.message.MessageManager;
import ru.porfirevalexey.networkchat.message.MessageMode;
import ru.porfirevalexey.networkchat.message.TextMessage;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

/**
 * Класс сервера межсетевого чата
 */
public class Server {

    private final String CONFIG_FILE = "server.cfg";
    private final String LOG_FILE = "logs.txt";

    private String host;
    private int port;
    private int bufferSize;
    private BufferedWriter logFile;

    protected ServerSocketChannel serverChannel;
    protected Selector selector;

    /**
     * Мэп, содержащий каналы сокетов и байтовые буферы подключенных пользователей
     */
    protected HashMap<SocketChannel, ByteBuffer> userSockets;
    /**
     * Мэп, содержащий каналы сокетов и имена подключенных пользователей
     */
    protected HashMap<SocketChannel, String> userNames;
    /**
     * Мэп, содержащий каналы сокетов и данные о возможности подключенных пользователей отправлять несервисные сообщения
     */
    protected HashMap<SocketChannel, Boolean> userCapable;

    /**
     * Создание и запуск нового сервера межсетевого чата
     */
    public Server() {
        start();
    }

    public static void main(String[] args) throws IOException {
        // запускаем сервер на обработку сообщений
        (new Server()).proceedMessages();
    }

    /**
     * Запуск сервера
     */
    private void start() {
        try {
            //читаем файл конфига
            readConfigurationFile(CONFIG_FILE);
            serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(host, port));
            //явно указывает режим неблокирующей работы сервера
            serverChannel.configureBlocking(false);

            selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            // открываем файл логов в папке с ресурсами
            logFile = new BufferedWriter(
                    new FileWriter(getClass()
                            .getResource("/" ).getPath() + LOG_FILE, true
                    )
            );
            log("INFO", "Сервер запущен...", true);
        } catch (IOException e) {
            log("ERROR", "Ошибка запуска сервера", true);
        }
        userSockets = new HashMap<>();
        userNames = new HashMap<>();
        userCapable = new HashMap<>();
    }

    /**
     * Обработка сообщений
     * @throws IOException Ошибка закрытия файла логов или серверного канала сокета
     */
    private void proceedMessages() throws IOException {
        try {
            while (true) {
                selector.select();
                for (SelectionKey event : selector.selectedKeys()) {
                    if (event.isValid()) {
                        if (event.isAcceptable()) {
                            acceptNewConnection();
                        } else if (event.isReadable()) {
                            receiveNewMessageFromUser(event);
                        } else if (event.isWritable()) {
                            sendMessageToUser(event);
                        }
                    }
                }
                selector.selectedKeys().clear();
            }
        } catch (IOException e) {
            log("ERROR", "Ошибка сервера при обработке сообщений", true);
        } finally {
            serverChannel.close();
            log("INFO", "Сервер завершил работу", true);
            logFile.close();
        }
    }

    /**
     * Отправка сообщения пользователю
     * @param event Объект, спровоцировавший новое событие на сервере
     */
    private void sendMessageToUser(SelectionKey event) {
        SocketChannel socketChannel = (SocketChannel) event.channel();
        ByteBuffer buffer = userSockets.get(socketChannel);
        //переходим в начало буфера
        buffer.flip();
        try {
            socketChannel.write(buffer);
            if (!buffer.hasRemaining()) {
                buffer.compact();
                // переводим в режим готовности читать
                socketChannel.register(selector, SelectionKey.OP_READ);
            }
        } catch (ClosedChannelException e) {
            log("ERROR", "Операция чтения/записи не возможна, так как канал уже закрыт", true);
        } catch (IOException e) {
            log("ERROR", "Ошибка операции записи содержимого буфера в поток", true);
        }
    }

    /**
     * Получение нового сообщения от пользователя и его обработка
     * @param event Объект, спровоцировавший новое событие на сервере
     * @throws IOException При ошибке передачи сообщений пользователям или при ошибке закрытия канала связи с ними
     */
    private void receiveNewMessageFromUser(SelectionKey event) throws IOException {
        SocketChannel socketChannel = null;
        try {
            socketChannel = (SocketChannel) event.channel();
            ByteBuffer buffer = userSockets.get(socketChannel);
            // сколько байт было прочитано
            int bytesCount = socketChannel.read(buffer);
            // в случае отключения клиента
            if (bytesCount == -1) {
                log("INFO", "Подключение с адресом " + socketChannel.getRemoteAddress() + " разорвано", true);
                // сообщаем всем остальным пользователям (кроме данного), что данный пользователь отключился от сервера
                Message msg = new TextMessage(userNames.get(socketChannel), "/exit");
                broadcastExceptOneUser(MessageManager.packMessageToByteArray(msg), socketChannel);
                //удаляем данные о пользователе из мэпов и закрываем его канал
                disconnectUser(socketChannel);
                return;
            }

            //переходим в начало буфера
            buffer.flip();
            // формируем сообщение из данных в буфере
            Message message = MessageManager.unpackMessageFromByteArray(buffer.array());
            // если это сервисное сообщение, то вызываем метод-обработчик сервисных сообщений
            // иначе обрабатываем его как стандартное сообщение
            if (isServiceMessage(message)) {
                proceedServiceMessage(socketChannel, message);
            } else {
                log("MESSAGE", message.toString(), true);
                // если есть данные и данному пользователю разрешено отправлять несервисные сообщения, то делаем массовую рассылку
                if (bytesCount > 0 && userCapable.get(socketChannel).equals(Boolean.TRUE)) {
                    broadcast(buffer.array());
                }
                // если есть данные и данному пользователю нельзя отправлять несервисные сообщения, то только очищаем буффер
                if(bytesCount > 0 && userCapable.get(socketChannel).equals(Boolean.FALSE)) {
                    buffer.clear();
                    // переводим в режим готовности писать
                    socketChannel.register(selector, SelectionKey.OP_WRITE);
                }
            }
        } catch (IOException e) {
            // если произошла ошибка чтения сообщения от пользователя, то отключаем его и сообщаем об этом остальным
            log("ERROR", "Ошибка чтения сообщения от пользователя: " + e.getMessage(), true);
            Message msg = new TextMessage(userNames.get(socketChannel), "/exit");
            broadcastExceptOneUser(MessageManager.packMessageToByteArray(msg), socketChannel);
            disconnectUser(socketChannel);
        } catch (ClassNotFoundException e) {
            log("ERROR", "Ошибка обработки сообщения от пользователя" + e.getMessage(), true);
        }
    }

    /**
     * Отключение пользователя от сервера путем закрытия его канала сокета и удаления его данных с сервера
     * @param socketChannel Канал сокета отключамого пользователя
     * @throws IOException В случае ошибки при закрытии канала сокета
     */
    protected void disconnectUser(SocketChannel socketChannel) throws IOException {
        log("INFO", "Пользователь " + userNames.get(socketChannel) + " покинул чат", true);
        userSockets.remove(socketChannel);
        userNames.remove(socketChannel);
        userCapable.remove(socketChannel);
        socketChannel.close();
    }

    /**
     * Массовая рассылка сообщения, представленного в байтовом виде
     * @param message Байтовое представление рассылаемого сообщения
     * @throws ClosedChannelException В случае, если какой-то из каналов сокетов был закрыт при работе метода
     */
    protected void broadcast(byte[] message) throws ClosedChannelException {
        for (Map.Entry<SocketChannel, ByteBuffer> entry : userSockets.entrySet()) {
            if(entry.getKey().isConnected()) {
                //очищаем буфер и записываем туда новые данные
                entry.getValue().clear();
                entry.getValue().put(ByteBuffer.wrap(message));
                // переводим в режим готовности записывать
                entry.getKey().register(selector, SelectionKey.OP_WRITE);
            }
        }
    }

    /**
     * Отправка сообщения одному конкретному пользователю, заданному его каналом сокета
     * @param socketChannel Канал сокета пользователя
     * @param message Передаваемое сообщение
     * @throws ClosedChannelException В случае, если канал сокета был закрыт при работе метода
     */
    protected void sendMessageToOneUser(SocketChannel socketChannel, byte[] message) throws ClosedChannelException {
        userSockets.get(socketChannel).clear();
        userSockets.get(socketChannel).put(ByteBuffer.wrap(message));
        socketChannel.register(selector, SelectionKey.OP_WRITE);
    }

    /**
     * Массовая рассылка сообщения, представленного в байтовом виде, всем пользователям на сервера за исключением одного
     * @param message Байтовое представление рассылаемого сообщения
     * @param socketChannel Канал сокета пользователя, которому не надо рассылать сообщение
     * @throws ClosedChannelException
     */
    protected void broadcastExceptOneUser(byte[] message, SocketChannel socketChannel) throws ClosedChannelException {
        for (Map.Entry<SocketChannel, ByteBuffer> entry : userSockets.entrySet()) {
            if (!entry.getKey().equals(socketChannel)) {
                entry.getValue().clear();
                entry.getValue().put(ByteBuffer.wrap(message));
                entry.getKey().register(selector, SelectionKey.OP_WRITE);
            }
        }
    }

    /**
     * Обработка нового подключения пользователя на сервер
     */
    protected void acceptNewConnection() {
        try {
            SocketChannel socketChannel = serverChannel.accept();
            //явно указываем неблокирующий режим работы
            socketChannel.configureBlocking(false);
            log("INFO", "Новое подключение с адреса " + socketChannel.getRemoteAddress(), true);
            // помещаем нового пользователя в мэп с данными о его буффере
            userSockets.put(socketChannel, ByteBuffer.allocate(bufferSize));
            socketChannel.register(selector, SelectionKey.OP_READ);
        } catch (IOException e) {
            log("ERROR", "Ошибка установки нового подключения", true);
        }
    }

    /**
     * Определение является ли сообщение сервисным
     * @param message Сообщение
     * @return true - если сообщение является сервисным, иначе false
     */
    private boolean isServiceMessage(Message message) {
        return MessageMode.SERVICE == message.getMessageMode();
    }

    /**
     * Обработка сервисного сообщения
     * @param socketChannel Канал сокета, с которого поступило сообщение
     * @param message Объект поступившего сервисного сообщения
     * @throws IOException в случае ошибок отправки сообщения пользователю или закрытия его канала сокета
     */
    protected void proceedServiceMessage(SocketChannel socketChannel, Message message) throws IOException {
        // разбиваем сервисное сообщение на две части - метка сервисного сообщение (/exit, /newUser и т.д.) и содержимое
        // этого сервисного сообщения, которое несет полезную информацию
        String[] messageParts = (new String(message.getContent())).split("\\s");
        switch (messageParts[0]) {
            // сообщение о выходе пользователя с сервера - рассылаем всем эту информацию и отключаем пользователя от сервера
            case "/exit":
                final ByteBuffer buffer = userSockets.get(socketChannel);
                broadcastExceptOneUser(buffer.array(), socketChannel);
                disconnectUser(socketChannel);
                break;
                // сообщение о подлючении нового пользователя к серверу
            case "/newUser":
                // проверяем нет ли пользователя с таким именем на сервере, если нет, то рассылаем всем информацию о его подключении
                // иначе только ему отправляем сервисное сообщение о том, что имя некорректно и переводим его в режим запрета
                // на массовую отправку несервисных сообщений
                if (isUserNameCorrect(message.getUsername())) {
                    log("INFO", "Пользователь " + message.getUsername() + " присоединился к чату", true);
                    broadcastNewUserConnectedToChat(socketChannel, message);
                } else {
                    log("INFO", "Пользователь " + message.getUsername() + " не смог присоединиться к чату", true);
                    this.userCapable.put(socketChannel, Boolean.FALSE);
                    // в сообщение отправляем текущий список пользователей, чтобы ему было видно, какие имена заняты
                    Message msg = new TextMessage(
                            message.getUsername(),
                            userNames
                                    .values()
                                    .stream()
                                    .reduce("/errorInitialUserName ", (subtotal, element) -> subtotal + ":" + element).getBytes(),
                            MessageMode.SERVICE);
                    sendMessageToOneUser(socketChannel, MessageManager.packMessageToByteArray(msg));
                }
                break;
                // сообщение о смене пользователем имени - смотрим, если вторая часть содержимого сервисного сообщения
            // содержит незанятое имя пользователя, то делем рассылку всем об изменении имени пользователя, иначе
            // сообщаем только этому пользователю, что он не смог сменить имя
            case "/changeName":
                if (isUserNameCorrect(messageParts[1])) {
                    log("INFO", "Пользователь " + message.getUsername() + " сменил ник на " + messageParts[1], true);
                    broadcastSuccessfulUserNameChange(socketChannel, messageParts[1]);
                } else {
                    log("INFO", "Пользователь " + message.getUsername() + " не смог сменить ник на " + messageParts[1], true);
                    // в сообщение отправляем имя, на которое не удалось сменить
                    Message msg = new TextMessage(
                            message.getUsername(),
                            ("/errorUserName " + messageParts[1]).getBytes(),
                            MessageMode.SERVICE);
                    sendMessageToOneUser(socketChannel, MessageManager.packMessageToByteArray(msg));
                }
                break;
        }
    }

    /**
     * Рассылка массового сообщения о смене пользователем имени
     * @param socketChannel Канал сокета пользователя, сменившего имя
     * @param userName Предыдущее имя пользователя
     * @throws IOException В случае ошибок отправки сообщения пользователю
     */
    protected void broadcastSuccessfulUserNameChange(SocketChannel socketChannel, String userName) throws IOException {
        // заменяем имя в мэпе на новое
        this.userNames.put(socketChannel, userName);
        // если пользователь не мог отправлять массовые несервисные сообщения, то переводим его в режим возможности
        // отправки таких сообщений и делаем массовую рассылку остальным пользователям о том, что он сменил имя. Ему же
        // отпраляем сообщение о том, что произошла смена ника
        // Иначе просто рассылаем сообщение о смене ника всем
        if(userCapable.get(socketChannel).equals(Boolean.FALSE)) {
            this.userCapable.put(socketChannel, Boolean.TRUE);
            Message msg = new TextMessage(getUserNameBySocketChannel(socketChannel), "/oldUserChangeName " + userName);
            broadcastExceptOneUser(MessageManager.packMessageToByteArray(msg), socketChannel);
            sendMessageToOneUser(socketChannel, userSockets.get(socketChannel).array());
        } else {
            broadcast(userSockets.get(socketChannel).array());
        }
    }


    /**
     * Рассылка массового сообщения о присоединении нового пользователя к серверу
     * @param socketChannel Канал сокета нового пользователя
     * @param message Сообщение, содержащее информацию о подключении нового пользователя
     * @throws IOException В случае ошибок отправки сообщения пользователям
     */
    protected void broadcastNewUserConnectedToChat(SocketChannel socketChannel, Message message) throws IOException {
        // добаляем пользователя в мэпы и даем ему возможность отправлять массовые несервисные сообщения
        this.userNames.put(socketChannel, message.getUsername());
        this.userCapable.put(socketChannel, Boolean.TRUE);
        // переходим в начало буффера
        this.userSockets.get(socketChannel).flip();
        //рассылаем всем кроме данного пользователя сообщение о новом подключении
        broadcastExceptOneUser(this.userSockets.get(socketChannel).array(), socketChannel);
        // самому пользователю отправляем список пользователей на сервере
        Message msg = new TextMessage(
                message.getUsername(),
                userNames
                        .values()
                        .stream()
                        .reduce("/userList ", (a, b) -> a + ":" + b).getBytes(),
                MessageMode.SERVICE);
        MessageManager.packMessageToByteArray(msg);
        userSockets.get(socketChannel).clear();
        userSockets.get(socketChannel).put(ByteBuffer.wrap(MessageManager.packMessageToByteArray(msg)));
        socketChannel.register(selector, SelectionKey.OP_WRITE);
    }

    /**
     * Проверка занято такое имя пользователя или нет
     * @param userName Проверяемое имя пользователя
     * @return true - если имя занято, иначе - false
     */
    protected boolean isUserNameCorrect(String userName) {
        return !userNames.containsValue(userName);
    }

    /**
     * Получение имени пользователя по его каналу сокета
     * @param socketChannel Канал сокета пользователя, которого надо найти на сервере
     * @return Имя пользователя
     */
    protected String getUserNameBySocketChannel(SocketChannel socketChannel) {
        return userNames.get(socketChannel);
    }

    /**
     * Чтение конфигурационного файла
     * @param fileName Имя конфигурационного файла
     */
    private void readConfigurationFile(String fileName) {
        try (BufferedReader br = new BufferedReader(
                new FileReader(
                        new File(getClass().getResource("/" + fileName).toURI())
                )
        )) {
            host = br.readLine().strip().split("=")[1];
            port = Integer.parseInt(br.readLine().strip().split("=")[1]);
            bufferSize = Integer.parseInt(br.readLine().strip().split("=")[1]);
        } catch (FileNotFoundException e) {
            log("ERROR", "Конфигурационный файл сервера\"" + fileName + "\" не найден", true);
        } catch (IOException | URISyntaxException e) {
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
                // не забываем сливать данные из буффера
                logFile.flush();
            } catch (IOException e) {
                log("ERROR", "Ошибка записи в файл логов");
            }
        }
    }
}

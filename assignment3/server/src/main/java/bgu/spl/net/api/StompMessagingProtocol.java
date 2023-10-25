package bgu.spl.net.api;

public interface StompMessagingProtocol<T> extends MessagingProtocol<T> {

    // void start(int connectionId, Connections<T> connections);
    // void process(T message);
    // boolean shouldTerminate();

    void sendError(int connectionId, String massage, String receiptId, String body);
    void sendConnect(int connectionId);
    void sendMessage(int connectionId, String destination, String body);
    void sendReceipt(int connectionId, String receiptId);

    void terminate();
}

package bgu.spl.net.api;

import bgu.spl.net.srv.Connections;

public interface ClientStompFrame<T> extends StompFrame {

    void process(int connectionId, Connections<T> connections, StompMessagingProtocol<T> protocol);
    boolean checkLegal();

    static Integer parseIntOrNull(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

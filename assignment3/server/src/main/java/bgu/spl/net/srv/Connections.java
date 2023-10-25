package bgu.spl.net.srv;

public interface Connections<T> {

    boolean send(int connectionId, T msg);

    void connect(ConnectionHandler<T> handler, int connectionId);

    void disconnect(int connectionId);

    srvData<T> Data();

}

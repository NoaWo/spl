package bgu.spl.net.srv;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionsImpl<T> implements Connections<T> {

    private srvData<T> data;
    private Map<Integer, ConnectionHandler<T>> idToHandler;

    public ConnectionsImpl() {
        this.data = new srvData<>();
        this.idToHandler = new ConcurrentHashMap<>(); 
    }

    @Override
    public boolean send(int connectionId, T msg) {
        
        ConnectionHandler<T> handler = idToHandler.get(connectionId);
        if (handler == null) 
            return false;
        try {
            handler.send(msg);
            return true;
        } catch (IOException e) {
            disconnect(connectionId);
            try {
                handler.close();
            } catch (IOException ex) {}
            return false;
        }
    }

    @Override
    public void connect(ConnectionHandler<T> handler, int connectionId) {
        idToHandler.put(connectionId, handler);
        handler.start(connectionId, this);
    }

    @Override
    public void disconnect(int connectionId) {
        idToHandler.remove(connectionId);

        User userDisconnected = data.findUserByConnectionId(connectionId); 

        if (userDisconnected != null) { // the client may didnt login yet
            Set<String> channels = userDisconnected.getChannels();
            for (String channel : channels) {
                Data().removeSubscriber(connectionId, channel);
            }

            userDisconnected.disconnect();
            data.removeUser(connectionId);
        }
    }

    @Override
    public srvData<T> Data() {
        return this.data;
    } 
}

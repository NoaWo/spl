package bgu.spl.net.impl.stomp;

import java.util.List;

import bgu.spl.net.api.ClientStompFrame;
import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.Connections;

public class SubscribeFrame extends StompFrameImp implements ClientStompFrame<String> {

    public SubscribeFrame(Command command) {
        super(command);
    }

    @Override
    public void process(int connectionId, Connections<String> connections, StompMessagingProtocol<String> protocol) {
        String destination = getHeaderValue("destination");
        int subscriptionId = Integer.parseInt(getHeaderValue("id"));

        boolean succeedSubscribe = false;

        List<Integer> subscribers = connections.Data().getSubscribers(destination);

        synchronized (connections.Data()) {
            if (subscribers == null) { // open new channel
                connections.Data().addNewChannel(destination, connectionId, subscriptionId);
                succeedSubscribe = true;
            }
        }

        if (!succeedSubscribe) { // add subscriber to exist channel
            if (!subscribers.contains(connectionId)) {
                connections.Data().addSubscriber(destination, connectionId, subscriptionId);
            }
        }

        String receiptId = getHeaderValue("receipt");
        if (receiptId != null)
            protocol.sendReceipt(connectionId, receiptId);
    }

    @Override
    public boolean checkLegal() {
        return hasHeader("destination") && hasHeader("id") && 
            ClientStompFrame.parseIntOrNull(getHeaderValue("id")) != null && !hasBody();
    }
    
}

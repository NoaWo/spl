package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.ClientStompFrame;
import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.User;

public class UnsubscribeFrame extends StompFrameImp implements ClientStompFrame<String> {

    public UnsubscribeFrame(Command command) {
        super(command);
    }

    @Override
    public void process(int connectionId, Connections<String> connections, StompMessagingProtocol<String> protocol) {
        int subscriptionId = Integer.parseInt(getHeaderValue("id"));
        String receiptId = getHeaderValue("receipt");

        User user = connections.Data().findUserByConnectionId(connectionId);
        if (user == null) { // should not enter here
            protocol.sendError(connectionId, "User is not connected", receiptId, null);
            return;
        }

        String channel = user.unsubscribe(subscriptionId);
        if (channel == null) {
            protocol.sendError(connectionId, "User have no such subscription", receiptId, "Subscription id " + subscriptionId + ".\n User have no such subscription id.");
            return;
        }
        connections.Data().removeSubscriber(connectionId, channel);
        
        if (receiptId != null)
            protocol.sendReceipt(connectionId, receiptId);
    }

    @Override
    public boolean checkLegal() {
        return hasHeader("id") && ClientStompFrame.parseIntOrNull(getHeaderValue("id")) != null && !hasBody();
    }
}

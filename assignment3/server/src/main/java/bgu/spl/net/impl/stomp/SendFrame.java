package bgu.spl.net.impl.stomp;

import java.util.List;

import bgu.spl.net.api.ClientStompFrame;
import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.User;

public class SendFrame extends StompFrameImp implements ClientStompFrame<String> {
    
    public SendFrame(Command command) {
        super(command);
    }

    @Override
    public void process(int connectionId, Connections<String> connections, StompMessagingProtocol<String> protocol) {
        User user = connections.Data().findUserByConnectionId(connectionId);

        String destination = getHeaderValue("destination");
        String receiptId = getHeaderValue("receipt");

        List<Integer> subscribers = connections.Data().getSubscribers(destination); // assume list is not chaged during SEND

        // Error cases
        if (subscribers == null) { // destination is not exist
            protocol.sendError(connectionId, "Destination is not exist", receiptId, "Destination: " + destination + ".\n This destination is not exist.");
        } 
        else if (!subscribers.contains(connectionId)) { // user is not subscribed
            protocol.sendError(connectionId, "User is not subscribed", receiptId, "Channel: " + destination + ".\n User "+ user.getUsername() + " is not subscribed to this channel.");
        }

        // handle send
        else {
            String body = getBody();
            for (Integer conId : subscribers) {
                protocol.sendMessage(conId, destination, body);
            }

            if (receiptId != null)
                protocol.sendReceipt(connectionId, receiptId);
        }
    }

    @Override
    public boolean checkLegal() {
        return hasHeader("destination");
    }
  

  
}

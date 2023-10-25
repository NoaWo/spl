package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.ClientStompFrame;
import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.Connections;

public class DisconnectFrame extends StompFrameImp implements ClientStompFrame<String> {

    public DisconnectFrame(Command command) {
        super(command);
    } 

    @Override
    public void process(int connectionId, Connections<String> connections, StompMessagingProtocol<String> protocol) {
        String receipt = getHeaderValue("receipt");
        if (receipt != null)
            protocol.sendReceipt(connectionId, receipt);

        protocol.terminate();
    }

    @Override
    public boolean checkLegal() {
        return hasHeader("receipt") && !hasBody();
    }
    
}

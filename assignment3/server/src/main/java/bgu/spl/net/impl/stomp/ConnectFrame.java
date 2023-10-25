package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.ClientStompFrame;
import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.User;

public class ConnectFrame extends StompFrameImp implements ClientStompFrame<String> {

    public ConnectFrame(Command command) {
        super(command);
    }

    @Override
    public void process(int connectionId, Connections<String> connections, StompMessagingProtocol<String> protocol) {
        String username = getHeaderValue("login");
        String password = getHeaderValue("passcode");

        String receiptId = getHeaderValue("receipt");

        boolean succeedConnect = false;

        synchronized (connections.Data()) {

            User user = connections.Data().findUserbyUsername(username);
            if (user == null) { // new user
                User newUser = new User(username, password, connectionId);
                connections.Data().addNewUser(newUser);
                succeedConnect = true;
            }
            else {
                if (!user.getPassword().equals(password)) { // wrong password
                    protocol.sendError(connectionId, "Wrong password", receiptId, null);
                }
                else {
                    if (user.getIsConnect()) { // user already logged in
                        protocol.sendError(connectionId, "User already logged in", receiptId, null);
                    }
                    else { // can connect
                        connections.Data().connectOldUser(connectionId, user);;
                        succeedConnect = true;
                    }
                }
            }
        }
        if (succeedConnect) {
            protocol.sendConnect(connectionId);

            if (receiptId != null)
                protocol.sendReceipt(connectionId, receiptId);
        }
    }

    @Override
    public boolean checkLegal() {
        return hasHeader("accept-version") && getHeaderValue("accept-version").equals("1.2") && hasHeader("host") && hasHeader("login")
                && hasHeader("passcode") && !hasBody();
    }
    
}

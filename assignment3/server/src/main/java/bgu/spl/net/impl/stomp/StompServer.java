package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.Server;

public class StompServer {

    public static void main(String[] args) {
        int port;
        try { 
            port = Integer.parseInt(args[0]); 
        } catch (NumberFormatException e) { 
            System.out.println("Illegal Arguments");
            return;
        }
        if (args[1].equals("tpc")) {
            Server.threadPerClient(
                port, //port
                () -> new StompMessagingProtocolImp(), //protocol factory
                StompEncoderDecoder::new //message encoder decoder factory
            ).serve();
        }
        else if (args[1].equals("reactor")) {
            Server.reactor(
                Runtime.getRuntime().availableProcessors(),
                port, //port
                () -> new StompMessagingProtocolImp(), //protocol factory
                StompEncoderDecoder::new //message encoder decoder factory
            ).serve();
        }
        else {
            System.out.println("Illegal Arguments");
            return;
        }
    }
}

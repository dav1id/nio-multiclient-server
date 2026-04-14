package v2;

import java.net.SocketAddress;

public class ClientMeta {
    private SocketAddress clientIdentifier;
    private String clientName;

    public ClientMeta(SocketAddress clientIdentifier, int clientIncrement){
        this.clientIdentifier = clientIdentifier;
        this.clientName = String.format("Client%d", clientIncrement);
    }

    public String getClientName(){
        return clientName;
    }
}

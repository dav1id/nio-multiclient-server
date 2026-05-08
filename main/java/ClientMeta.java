package main.java;

import java.net.SocketAddress;

public class ClientMeta {
    private SocketAddress clientIdentifier;
    private String clientName;

    public ClientMeta(SocketAddress clientIdentifier, int clientIncrement){
        this.clientIdentifier = clientIdentifier;

        if (clientIncrement > 0){
            this.clientName = String.format("Client%d", clientIncrement);
        } else {
            this.clientName = "Server";
        }
    }

    public String getClientName(){
        return clientName;
    }

    public void setClientName(String clientName){
        this.clientName = clientName;

    }}

/*
    Need a way to deal with messages going into the server by creating some kind of buffer.
        1. Server can behave like the output and input link inside of a router possibly (?)


    Server waits for input. If it receives input it's going to have to decode the message, and then notify
    the specific client's receiver that a message is going to be sent.

    I need a routing protocol and output buffer so that one of my threads isn't constantly congested. I'm going to create
    three threads that will handle server Input. Each server input thread will notify the server of the state of their
    buffer, and the server will redirect the message to the server thread that has the lowest number of messages
    to process in their buffer.

    I need the same routing protocol for the server output to the other clients.
    Also need a way to send server messages from the serverOutput.

    Server is going to save the client into a static hashmap. Each Client will literally be called CLientOne, ClientTwo,
    etcetra.

    The specific Server client is going to create three paths that a message can follow. Each path is going to notify
    the server of how full its buffer is
*/

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.concurrent.TimeUnit;

public class ServerInit {

    // Server has to be sending information about the number of clients to each client that is connected
    public ServerInit(){

    }

    public static void main(String[] args){

        BufferedReader in = null;
        try(ServerSocket serverSocket = new ServerSocket(8080);
            Socket clientSocket = serverSocket.accept();
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ){

            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));


            try {
                while (!(in.ready())) {
                    TimeUnit.SECONDS.sleep(3);
                    System.out.println("Waiting to receive input from client....");

                }
            } catch(InterruptedException e){
                System.out.println(e.getMessage());
            }
            String message = in.readLine();


            // Testing out the PacketTransceiver:
            System.out.println("Testing Packet Transceiver: ");
            System.out.println(message);

            // Testing out the PacketReceiver:
            System.out.println("Testing Packet Receiver: ");
            out.println("Hello, Client!");


        } catch(IOException e){
            System.out.println(e.getMessage());
        }
    }

    /**
     Message that's sent from the PacketTransceiver is going to have the format of
     (user) (message)

     Going to need to specify which user it's sending the message to

     **/
    public void decodeMessage(){

    };

}
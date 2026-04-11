import javax.swing.*;
import java.io.PrintWriter;
import java.net.Socket;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Scanner;

import java.util.ArrayList;

import java.io.IOException;

public class PacketTransceiver implements Runnable {
    private final Socket socket;
    private PacketReceiver packetReceiver;

    private final String [] args;
    private ArrayList<String> clientsList = new ArrayList<>();

    private boolean status = false;
    private final Object lock;

    private Queue<String> messageQueue;

    /**
     Initialises the PacketTransceiver that will act as a thread once run() is called.
     Socket is the client socket that we'll be using to communicate with the server,

     Args is going to be unique arguments for the message. Like a client's messageLimit etcetra
     numClients will be the number of clients that is going to be updated depending on a server message.
     **/

    public PacketTransceiver(PacketReceiver packetReceiver, Socket socket, String[] args){
        this.socket = socket;
        this.args = args;
        this.lock = Client.lock;

        messageQueue = new ArrayDeque<>();
    }

    /**
     Client is closed when the transceiver receives the input to close the server by using the notify method.
     **/

    public void closeClient(){
        synchronized (lock){
            lock.notify();
        }
    }

    /**
        Allows only one thread to call setServerMessage or checkMessage at a time. In case one thread is trying to
        change the list of clients, while another one is using old info to check if the message can be sent. Ideally
        the thread sending a server message should have more priority -> Limitation of my design that I've been trying
        to find a solution to. The server just won't send the message if the client does not exist instead of making an
        exception.
     **/

    /**
     Waits for the thread to receive input from the server. Can receive input as:
     1. Input from a different client
     2. Input from the receiver. The receiver is going to instantiate by creating a path to the PacketTransceiver.
     It's going to call the PacketTransceiver method changeClientNumber

     2. Input in terms of a server message.
     **/
    public void run(){
        status = true;
        try(
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                Scanner scanner = new Scanner(System.in)
        ){
            while (status){
                String message;
                while((message = scanner.nextLine()) != null){
                    if (!(message.equals("close"))){
                        out.println(message);
                    } else {
                       status = false;
                       packetReceiver.setStatus(false);
                    }
                }
            }

            closeClient();
            System.out.println("Closing the client per user request");

        } catch(IOException e){
            System.out.println(e.getMessage());
        }
    }
}
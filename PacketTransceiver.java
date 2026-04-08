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
    public synchronized boolean messageFunc(String message, String command){
        if (command.equals("setServerMessage")){
            return setServerMessage(message);
        } else{
            return checkMessage(message);
        }
    }

    /**
        PacketReceiver forwards the message from the server. i.e., a message like:
            "server -UpdateClients -remove client1 client2 client 3 -add client1 client2 client3"

        Only message that the server can send so far is a notification to all clients to change the number of clients
        when a client leaves or joins.
     **/
    public boolean setServerMessage(String serverMessage) {
        String[] concat = serverMessage.split("-");

        switch(concat[1]){
            case "serverUpdateClients":
                String[] addList = (concat[1]).split(" ");
                String[] removeList = (concat[2]).split(" ");

                for (String rmv : removeList)
                    clientsList.remove((String) rmv);

                for (String add : addList)
                    clientsList.add((String) add);

                return true;

            default:
                return false;
        }
    }

    /**
     Checks if the message aligns with the format. The message should contain a client that exists in the clientsList
     **/
    public boolean checkMessage(String message){
        String[] messageSeperator = message.split(" ", 1);

        boolean correctMessage = clientsList.contains((String) messageSeperator[0]);

        if (correctMessage) return true;
        return false;
    }


    public synchronized void queueFunctions(String message, String function){
        switch(function){
            case "add":
                messageQueue.add(message);
            case "remove":
                messageQueue.remove();
                break;
            default:
                break;
        }
    }

    /**
     Waits for the thread to receive input from the server. Can receive input as:
     1. Input from a different client
     2. Input from the receiver. The receiver is going to instantiate by creating a path to the PacketTransceiver.
     It's going to call the PacketTransceiver method changeClientNumber

     2. Input in terms of a server message.
     **/
    public void run(){
        try(
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                Scanner scanner = new Scanner(System.in)
        ){

            Thread inputThread = new Thread(() -> {
                String message;

                while (!(message = scanner.nextLine()).equals("close")) {
                    synchronized(this) {
                        if (checkMessage(message)) {
                            queueFunctions(message, "add");
                        } else {
                            System.out.println("Message cannot be sent to someone who is offline");
                        }
                    }
                }

                packetReceiver.setStatus(false);
            });
            inputThread.start();

            while (true){
                String message;
                if ((message = messageQueue.peek()) != null){
                    if (message.equals("close")) break;

                    if (checkMessage(message)){
                        out.println(message);
                        synchronized(this) {
                            queueFunctions(message, "remove");
                        }
                    } else {
                        System.out.println("This message cannot be sent because this client does not exist"); // Will be handled by back-end once the GUI is running
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
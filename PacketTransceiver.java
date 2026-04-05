import java.io.PrintWriter;
import java.net.Socket;
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
     Server is going to send a message to PacketReceiver when the number of clients have been changed.
     The PacketReceiver is going to decode the server message, and have a reference to PacketTransceiver
     to change the number of clients list that is necessary for the PacketTransceiver to encode its message.

     Might want to look into making it into a switch statement

     Server is going to send a message with remove followed by the clients to remove. And add, followed by the clients to add.
     Using the format -> "server-UpdateClients-remove client1 client2 client 3 - add client 1 client 2 client 3"
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

                return true; // Client understands message and responds to PacketReceiver by sending true
        }
        return false;
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

            ArrayList<String> messageList = new ArrayList<>();

            Thread inputThread = new Thread(() -> {
                String message;

                while (!(message = scanner.nextLine()).equals("close")) {
                    synchronized(this) {
                        messageList.add(message);
                    }
                }
            });
            inputThread.start();

            while (true){ //Look into finding a way to pause this as changeByServer is running
                if (!(messageList.isEmpty())){
                    synchronized(this) {
                        String message = messageList.getFirst();
                        if (message.equals("close")) break;

                        if (checkMessage(message)) {
                            out.println(message);
                            messageList.removeFirst();
                        } else {
                            System.out.println("This message cannot be sent because this client does not exist"); // Will be handled by back-end once the GUI is running
                        }
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
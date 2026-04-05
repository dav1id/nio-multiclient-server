import java.io.IOException;
import java.net.Socket;

public class Client {
    /**
     Initialise the client and make it connect to the server, create a thread for writing user response, and continue
     waiting for server response
     **/

    public PacketTransceiver packetTransceiver;
    public PacketReceiver packetReceiver;

    private final String serverAddress;
    private final int portNumber;

    public static final Object lock = new Object(); // Defining an object that is going to allow me to use the wait(). Wait() requires synchronized to only allowed one thread to use the wait on another thread

    public Client(String serverAddress, int portNumber){
        this.serverAddress = serverAddress;
        this.portNumber = portNumber;
    }

    /**
     Creates the client socket that connects to the server. Client closes when its PacketReceiver status
     returns, "Not Operational". It checks for this status message evert fifteen seconds in a while loop.
     **/

    public void run(){
        String[] args = new String[3];

        try(Socket socket = new Socket(serverAddress, portNumber)){

            // Defining an object that is going to allow me to use the wait(). Wait() requires synchronized to only allow one thread to use the wait on another thread

            this.packetTransceiver = new PacketTransceiver(packetReceiver, socket, args);
            Thread packetTransceiverThread = new Thread(packetTransceiver);
            packetTransceiverThread.start();

            this.packetReceiver = new PacketReceiver(this.packetTransceiver, socket);
            Thread packetReceiverThread = new Thread(packetReceiver);
            packetReceiverThread.start();

            try {
                synchronized (lock){
                    lock.wait();
                }

            } catch(InterruptedException e){
                System.out.println(e.getMessage());
            }

            System.out.println("Closing out of this client");


        } catch (IOException e){
            System.out.println(e.getMessage());
        }
    }


    public static void main(String[] args){
        String serverAddress = "127.0.0.1";
        int portNumber = 8080;

        Client client = new Client(serverAddress, portNumber);
        client.run();
    }
}
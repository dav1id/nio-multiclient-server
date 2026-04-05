import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

import java.net.Socket;
import java.io.IOException;

public class PacketReceiver implements Runnable {
    private PacketTransceiver packetTransceiver = null;
    private Socket socket;
    private String message;

    private boolean status;


    public PacketReceiver(PacketTransceiver packetTransceiver, Socket socket){
        this.packetTransceiver = packetTransceiver;
        this.socket = socket;
    }

    public void setStatus(){
        status = false;
    }


    /*
        I can pass the lock in PacketReceiver as well. Once the client receives the response from its wait() (client
        is closed), the client is going to send a notify to run here.

       I can create a second thread inside of the PacketReceiver that incrementally checks a boolean value assigned to
       PacketTransceiver. If PacketTransceiver sees the

       Pass a reference to PacketReceiver from PacketTransceiver
    */
    public void run(){
        status = true;
        try(BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))){
            while (status){
                String message;
                if ((message = in.readLine()) != null){
                    String[] concat = message.split("-", 1);

                    if (concat[0].equals("server")){
                        if (!(packetTransceiver.setServerMessage(message))){
                           System.out.println("Invalid server message...."); //Find out what to do here, don't want to create an outputstream here. Might need to create a PacketTransceiver method to send messages.
                        }
                    } else{
                        System.out.println(message);
                    }
                }
            }
        } catch (IOException e){
            Thread.currentThread().interrupt();
            System.out.println("Packet Receiver Error: Client cannot sucessfully receive input from the server.");
        }
    }
}
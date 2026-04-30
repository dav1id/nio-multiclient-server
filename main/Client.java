package main;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Client implements Runnable {
    private final ArrayList<String> clientsList = new ArrayList<>();
    private final String senderName = "sender-thread";
    private final String receiverName = "receiver-thread";

    private final String[] threadPriority = {senderName, receiverName}; // set a thread priority for the receiver and not sender
    private synchronized boolean interactClientsList(String threadName, String message){
        switch(threadName){
            case senderName:
                String[] temp = message.split(" ");
                temp[0] = temp[0].replace("[", "");
                temp[temp.length - 1] = temp[temp.length - 1].replace("]", "");

                clientsList.clear();
                clientsList.addAll(List.of(temp));
                break;
            case receiverName:
                return clientsList.contains(message);
        }
        return false;
    }

    /*
        Need to create a temporary thread here? Look into it and the cost. It needs to set or remove depending on the
        serverMessage that was sent to remove/add
   */

    public final void clientReceiver(SocketChannel channel){
        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        String threadName = Thread.currentThread().getName();

        try {
            while(true){
                int result = channel.read(readBuffer);
                if (result == -1) break;

                if (!(result == 0)){
                    readBuffer.flip();

                    byte[] bytes = readBuffer.array();
                    String message = new String(bytes, 0, readBuffer.limit());
                    String[] messageSplit = message.split(" ", 2);

                    if (messageSplit[0].equals("server"))
                        Thread.startVirtualThread( () -> interactClientsList(threadName, messageSplit[1]));

                    System.out.printf("%s -> Me: %s %n" , messageSplit[0], messageSplit[1]);
                }

                readBuffer.clear();
            }
        } catch(IOException e){
          System.out.println(e.getMessage());
        }
    }

    public final void clientSender(SocketChannel channel){
        ByteBuffer writeBuffer = ByteBuffer.allocate(1024);
        Thread.currentThread().setName(senderName);

        try(Scanner in = new Scanner(System.in)){
            while(channel.isConnected()) {
                String message;
                if (in.hasNext() && (message = in.nextLine()) != null) {
                    writeBuffer.clear();

                    String[] messageArray = message.split(" ", 2);
                    if(message.equals("this close")) break;

                    //System.out.printf("Me -> %s: %s %n", messageArray[0], messageArray[1]);

                    if ((messageArray.length >= 2) && interactClientsList(senderName, messageArray[1])) {
                        byte[] messageBytes = message.getBytes();

                        writeBuffer.put(messageBytes);
                        writeBuffer.flip();

                        while (writeBuffer.hasRemaining())
                            channel.write(writeBuffer);

                        writeBuffer.clear();
                        writeBuffer.flip();
                    } else {
                        System.out.println("Message cannot be sent to server...");
                    }
                }
            }
        } catch(IOException e){
            System.out.println(e.getMessage());
        }
    }

    public void run(){
        try(final SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(8080))){
            Thread receiverThread = new Thread( () -> clientReceiver(socketChannel));
            receiverThread.start();
            receiverThread.setName(receiverName);

            clientSender(socketChannel);
        } catch(IOException e){
            System.out.println(e.getMessage());
        }
    }
}
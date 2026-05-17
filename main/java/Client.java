package main.java;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;

public class Client implements Runnable {
    private final ArrayList<String> clientsList = new ArrayList<>();

    private final String receiverName = "receiver-thread";

    //Controller
    private MessageLock messageLock;

    /**
     * The sender and receiver threads both call interactClientsList when it's trying to send a message. This makes sure that
     * both the sender and receiver are working with an updated clients list. The receiver will be the only one enabling
     * updateList to be true if the server sends it a message.
     **/
    private synchronized void interactClientsList(Boolean updateList, String message) {
        if (updateList) { // Assuming that the thing calling updateList is going to be the receiver parsing a server message
            String[] temp = message.split(",");
            temp[0] = temp[0].replace("[", "");
            temp[temp.length - 1] = temp[temp.length - 1].replace("]", "");

            clientsList.addAll(Arrays.asList(temp));
            clientsList.replaceAll(client -> client.replace(" ", ""));
        }
    }

    private void clientReceiver(SocketChannel channel) {
        ByteBuffer readBuffer = ByteBuffer.allocate(1024);
        String threadName = Thread.currentThread().getName();

        try {
            while (true) {
                int result = channel.read(readBuffer);
                if (result == -1) break;

                if (!(result == 0)) {
                    readBuffer.flip();

                    byte[] bytes = readBuffer.array();
                    String message = new String(bytes, 0, readBuffer.limit());
                    String[] messageSplit = message.split(" ", 2);

                    if (messageSplit[0].equals("server")) {
                        Thread.startVirtualThread(() -> interactClientsList(true, messageSplit[1]));
                    } else {
                        System.out.printf("%s -> Me: %s %n", messageSplit[0], messageSplit[1]);
                    }
                }
                readBuffer.clear();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void clientSender(SocketChannel channel) {
        ByteBuffer writeBuffer = ByteBuffer.allocate(1024);

        System.out.println("Sender operational?");
        while (channel.isConnected()) {
            String message = "";
            synchronized(messageLock){
                try {
                    messageLock.wait();
                    message = messageLock.getMessage();

                } catch(InterruptedException e){
                    System.out.println(e.getMessage());
                }
            }

            try {
                writeBuffer.clear();

                String[] messageArray = message.split(" ", 2);

                if (message.equals("this close")) break;

                interactClientsList(false, null);

                if ((messageArray.length >= 2) && clientsList.contains(messageArray[0])) {
                    System.out.printf("Me -> %s: %s %n", messageArray[0], messageArray[1]);

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
            } catch(IOException e){
                System.out.println(e.getMessage());
            }
        }
    }

    public void setMessageLock(MessageLock messageLock){
        this.messageLock = messageLock;
    }

    public void run(){
        try(final SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(8080))){
            Thread receiverThread = new Thread( () -> clientReceiver(socketChannel));
            receiverThread.setName(receiverName);
            receiverThread.start();

            clientSender(socketChannel);

        } catch(IOException e){
            System.out.println(e.getMessage());
        }
    }
}
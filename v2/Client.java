package v2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class Client implements Runnable {
    public final void clientReceiver(SocketChannel channel){
        ByteBuffer writeBuffer = ByteBuffer.allocate(1024);

        try {
            while(channel.isConnected()) {
                if (channel.read(writeBuffer) != -1) {
                    writeBuffer.flip();

                    byte[] bytes = writeBuffer.array();
                    String message = new String(bytes, 0, writeBuffer.limit());
                    System.out.println(message);
                }

                writeBuffer.clear();
                writeBuffer.flip();
            }

        } catch(IOException e){
          System.out.println(e.getMessage());
        };

    }

    public final void clientSender(SocketChannel channel){
        ByteBuffer readBuffer = ByteBuffer.allocate(1024);

        try(Scanner in = new Scanner(System.in)){
            while(channel.isConnected()) {
                String message;
                if (in.hasNext() && (message = in.nextLine()) != null) {
                    String[] messageArray = message.split(" ", 2);
                    System.out.println(messageArray[1]);

                    if ((messageArray.length > 1)) {
                        byte[] messageBytes = message.getBytes();

                        readBuffer.put(messageBytes);
                        readBuffer.flip();

                        //DEBUG
                        System.out.printf("Sent the message: %s...%n", message);

                        while (readBuffer.hasRemaining())
                            channel.write(readBuffer);

                        readBuffer.clear();
                        readBuffer.flip();

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
            Thread clientThread = new Thread( () -> clientReceiver(socketChannel));
            clientThread.start();

            clientSender(socketChannel);

        } catch(IOException e){
            System.out.println(e.getMessage());
        }
    }
}
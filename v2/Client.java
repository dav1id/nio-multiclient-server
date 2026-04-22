package v2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class Client implements Runnable {
    public final void clientReceiver(SocketChannel channel){
        ByteBuffer readBuffer = ByteBuffer.allocate(1024);

        try {
            while(true){
                int result = channel.read(readBuffer);
                if (result == -1) break;

                if (!(result == 0)){
                    readBuffer.flip();

                    byte[] bytes = readBuffer.array();
                    String message = new String(bytes, 0, readBuffer.limit());
                    String[] messageSplit = message.split(" ", 2);
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

        try(Scanner in = new Scanner(System.in)){ // Way to make System.in not print what I'm saying exactly for more cinematic effect...
            while(channel.isConnected()) {
                String message;
                if (in.hasNext() && (message = in.nextLine()) != null) {
                    writeBuffer.clear();

                    String[] messageArray = message.split(" ", 2);
                    if(message.equals("this close")) break;

                    //System.out.printf("Me -> %s: %s %n", messageArray[0], messageArray[1]);

                    if ((messageArray.length >= 2)) {
                        byte[] messageBytes = message.getBytes();

                        writeBuffer.put(messageBytes);
                        writeBuffer.flip();

                        //DEBUG START
                      //  System.out.printf("Sent the message: %s...%n", message);
                        //DEBUG END

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
            Thread clientThread = new Thread( () -> clientReceiver(socketChannel));
            clientThread.start();

            clientSender(socketChannel);

        } catch(IOException e){
            System.out.println(e.getMessage());
        }
    }
}
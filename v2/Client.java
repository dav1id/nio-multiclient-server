package v2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class Client implements Runnable {

    public void receiver(){
        // Printing out the contents of the channel if it recieves information
    }

    public void byteBufferFunc(){
        // Will use a synchronized lock to make both the receiver and sender functions share the same byte buffer
    }

    public void run(){
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        try(SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(8080));
            Scanner in = new Scanner(System.in)){

            String message = in.nextLine();

            byte[] messageBytes = message.getBytes();
            buffer.put(messageBytes);
            buffer.flip();

            while(buffer.hasRemaining()){
                socketChannel.write(buffer);
            }

            buffer.clear();

            /*
            while(true){
                String message;
                if (in.hasNext() && (message = in.nextLine()) != null){
                    buffer.clear();

                    byte[] messageBytes = message.getBytes();
                    buffer.put(messageBytes);
                    buffer.flip();

                    //DEBUG
                    System.out.printf("Sent the message: %s...%n", message);

                    while(buffer.hasRemaining()){

                            Writes the buffer by 'streaming' from one channel to another. Since it's non-blocking,
                            need to verify that buffer has completed the entire stream of bits before closing

                        socketChannel.write(buffer);
                    }

                    System.out.println("Wrote the message to server channel...");

                    buffer.clear();
                    buffer.flip();
                }
            } */

        }  catch(IOException e){
            System.out.println(e.getMessage());
        }
    }
}

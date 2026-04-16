package v2;

/*
   First-Level: Loops through a list of selectionKeys, identifies the server selection channel and checks if a client
   is waiting to be accepted. Checks if other client channels are waiting to be read or written from. Generates a task
   and submits it to a ThreadPool as a ConsumerTask (reading) and a ProducerTask (writing).
*/

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/*
    I pass in a reference of selectionKeySet, but selectionkeySet can change immediately after
*/
public class Server implements Runnable { // way to identify if a channel has disconnected
    private boolean status;
    public int clientCounter = 0;

    /**
        Receives user message, and forwards it to the appropiate client by creating a new specific task that omits
        iterating over the selectionKeys, called producerTaskWithoutIteration.
     **/
    public boolean consumerTask(Set<SelectionKey>  selectionKeys, SelectionKey sender, ExecutorService producerThreadPool) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        try(SocketChannel client = (SocketChannel) sender.channel()){
            int result;
            do {
                result = client.read(buffer);

                if (result == 0){
                    throw new RuntimeException(); // Don't know what to do here
                }
            } while(result != -1);

            String message = new String(buffer.array(), 0, buffer.limit());
            String[] messageArray = message.split(" ", 1);

            for(SelectionKey receiver : selectionKeys){
                ClientMeta clientMeta = (ClientMeta) receiver.attachment();

                if (clientMeta.getClientName().equals(messageArray[0])){

                    producerThreadPool.submit(
                            () -> {
                                unicastProducerTask(sender, receiver, messageArray[1]); // add condition to check if message can be divided to 1 and 2  -> client2 hello how are you
                            }
                    );
                    return true;
                }
            }

        } catch(IOException e){
            System.out.println(e.getMessage());
        }

        return false;
    }

    public boolean unicastProducerTask(SelectionKey senderKey, SelectionKey receiverKey, String message){
        /*
         A specific thread will both share the same ByteBuffer when I make threadLocal. For now, .flip() and .clear() might seem useless.
         I was thinking of making it reference the ByteBuffer but I want it compartamentalised
        */
        if (!(receiverKey.isWritable())) return false;


        ClientMeta senderMeta = (ClientMeta) senderKey.attachment();
        ClientMeta receiverMeta = (ClientMeta) receiverKey.attachment();
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);


        try(SocketChannel receiver = (SocketChannel) receiverKey.channel()){
            String formattedMessage = String.format("%s: %s", senderMeta.getClientName(), message);
            byte[] buffer = formattedMessage.getBytes();

            byteBuffer.put(buffer);
            byteBuffer.flip();

            while(byteBuffer.hasRemaining()){
                receiver.write(byteBuffer);
            }

            //DEBUG
            String debug = String.format("Finished printing %s.... to %s", message, receiverMeta.getClientName());
            System.out.println(debug);

        } catch(IOException e){
            String errMessage = String.format("Error trying to send information from %s to %s", senderMeta.getClientName(), receiverMeta.getClientName());
            System.out.println(errMessage);
        }

        return true;
    }

    /*
        Future way to simulate broadcast from client to all, and from server to all. Server to all can be a way to change
        some aspects of the visual interface when I start using JavaFX.
    */

    public boolean broadcastProducerTask(Set<SelectionKey> selectionKeys, SelectionKey sender, String broadcastMessage){
        return true;
    }

    /*
        Future way to simulate sending messages to multiple clients from one client. Will probably happen after
        I create the group chat using JavaFX
    */
    public boolean multicastProducerTask(){
        return false;
    }

    public void run(){
        status = true;
        try(
                ExecutorService producerThreadPool = Executors.newFixedThreadPool(3);
                ExecutorService consumerThreadPool = Executors.newFixedThreadPool(3);

                Selector selector = Selector.open();
                ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()){

            serverSocketChannel.bind(new InetSocketAddress(8080));
            serverSocketChannel.configureBlocking(false);
            SelectionKey serverKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            serverKey.attach(new ClientMeta(serverSocketChannel.getLocalAddress(), clientCounter));
            clientCounter++;

            while(true){
                selector.select();

                Set<SelectionKey> selectionKeySet = selector.selectedKeys();
                Iterator<SelectionKey> selectedKeyIterator = selectionKeySet.iterator();

                while(selectedKeyIterator.hasNext()){
                    var selectKey = selectedKeyIterator.next();

                    //DEBUG
                    ClientMeta meta = (ClientMeta) selectKey.attachment();
                    System.out.println(meta.getClientName());
                    System.out.println("___________________");

                    selectedKeyIterator.remove();

                    if (selectKey.isAcceptable()){
                        System.out.println("accepting client");

                        try{
                            SocketChannel client = serverSocketChannel.accept();

                            client.configureBlocking(false);
                            SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                            clientKey.attach(new ClientMeta(client.getLocalAddress(), clientCounter));

                            //DEBUG
                            meta = (ClientMeta) clientKey.attachment();
                            System.out.printf("%s has been connected! %n", meta.getClientName());
                            clientCounter++;
                        } catch(IOException e){
                            System.out.println(e.getMessage());
                        }
                    } else if (selectKey.isReadable()){
                        System.out.println("There is a client right now trying to be read...");
                        consumerThreadPool.submit( () -> {
                            consumerTask(selectionKeySet, selectKey, producerThreadPool);
                            // Space for some future backlog
                        });
                    }
                }
            }

        } catch(IOException e){
            System.out.println(e.getMessage());
        }
    }
}
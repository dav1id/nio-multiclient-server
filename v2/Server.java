package v2;

/*
   First-Level: Loops through a list of selectionKeys, identifies the server selection channel and checks if a client
   is waiting to be accepted. Checks if other client channels are waiting to be read or written from. Generates a task
   and submits it to a ThreadPool as a ConsumerTask (reading) and a ProducerTask (writing).
*/

import java.io.IOException;
import java.net.InetSocketAddress;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable { // way to identify if a channel has disconnected
    private boolean status;
    public int clientCounter = 0;


    /**
        Test method in trying to fix the bug of me passing a value that is changing from one thread to another thread.
        I don't want to slow down the server's process of updating selectionKeys. So thinking of a way
        for the threads in the threadpool to verify that their thread is up to date. But will go over this later after
        I've implemented a way for the server to know of a disconnected channel.
     **/
    public synchronized void changeTaskSet(){

    };

    /**
        Sends a message to the correct receiver by calling  producerTaskWithoutIteration once it's found the client.
     **/
    public boolean consumerTask(Set<SelectionKey>  selectionKeys, SelectionKey sender, ExecutorService producerThreadPool) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        try{
            SocketChannel client = (SocketChannel) sender.channel();
                int result;
                do {
                    result = client.read(buffer);

                    if (result == 0){
                        //DEBUG
                        System.out.printf("Cannot read to bufer.... consumerTaask - %s", ( (ClientMeta) sender.attachment()).getClientName());
                    }
                } while(result != -1);

                String message = new String(buffer.array(), 0, buffer.limit());
                String[] messageArray = message.split(" ", 1);

                for(SelectionKey receiver : selectionKeys){
                    ClientMeta clientMeta = (ClientMeta) receiver.attachment();

                    if (clientMeta.getClientName().equals(messageArray[0])){

                        producerThreadPool.submit(
                                () -> {
                                    if (!(unicastProducerTask(sender, receiver, messageArray[1]))){
                                        //DEBUG AND EVENTUAL FIX
                                        System.out.println("Producer task could not do work...");
                                    }
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
            // Need a specific exception here to write to the sender that the receiver does not exist. But a try and catch is expensive
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
                            if( !(consumerTask(selectionKeySet, selectKey, producerThreadPool)) ){
                                System.out.println("message could not be sent");
                            };

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
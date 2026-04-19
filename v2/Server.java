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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable { // way to identify if a channel has disconnected
    private boolean status;
    public int clientCounter = 0;

    private ArrayList<SelectionKey> registeredClientsList = new ArrayList<>();


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
    public void consumerTask(byte[] messageBytes, SelectionKey senderKey, ExecutorService producerThreadPool) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        String message = new String(messageBytes, 0, buffer.limit());
        String[] messageArray = message.split(" ", 2);

        String senderName =  ( (ClientMeta) senderKey.attachment() ).getClientName();

        if (!(message.equals("this close"))){
            for (SelectionKey receiver : registeredClientsList) {
                ClientMeta clientMeta = (ClientMeta) receiver.attachment();

                if (clientMeta.getClientName().equals(messageArray[0])) {

                    producerThreadPool.submit(
                            () -> {
                                if (!(unicastProducerTask(senderName, receiver, messageArray[1])))
                                    System.out.println("Producer task could not do work...");
                            }
                    );
                }
            }
        } else {
            closeLocalChannel(senderKey);
        }
    }

    public boolean unicastProducerTask(String senderName, SelectionKey receiverKey, String message){
        if (!(receiverKey.isWritable())) return false;

        ClientMeta receiverMeta = (ClientMeta) receiverKey.attachment();
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

        try{
            SocketChannel receiver = (SocketChannel) receiverKey.channel();

            String formattedMessage = String.format("%s: %s", senderName, message);
            byte[] buffer = formattedMessage.getBytes();

            byteBuffer.put(buffer);
            byteBuffer.flip();

            while(byteBuffer.hasRemaining()){
                receiver.write(byteBuffer);
            }

            //DEBUG
            String debug = String.format("Finished printing %s.... to %s %n", message, receiverMeta.getClientName());
            System.out.println(debug);

        } catch(IOException e){
            // Need a specific exception here to write to the sender that the receiver does not exist. But a try and catch is expensive
            String errMessage = String.format("Error trying to send information from %s to %s %n", senderName, receiverMeta.getClientName());
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

    /**
        Closes the local channel after the remote channel has been closed. The remote channel sends a specific
        server message tht indicates that the channel should be closed. (Likely just going to be this close).

        @param key Reference to the selection key that is going to be closed
     **/
    public void closeLocalChannel(SelectionKey key){
        SocketChannel channel = (SocketChannel) key.channel();

        try {
            channel.close();
        } catch(IOException e){
            System.out.printf("Problem closing the %s. System message: %s",  ( (ClientMeta) key.attachment() ).getClientName(), e.getMessage());
        }
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

                    ClientMeta meta = (ClientMeta) selectKey.attachment();

                    selectedKeyIterator.remove();

                    if (selectKey.isAcceptable()){
                        try{
                            SocketChannel client = serverSocketChannel.accept();

                            client.configureBlocking(false);
                            SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                            clientKey.attach(new ClientMeta(client.getLocalAddress(), clientCounter));
                            
                            //DEBUG START
                            meta = (ClientMeta) clientKey.attachment();
                            System.out.printf("%s has been connected! %n", meta.getClientName());
                            //DEBUG END

                            clientCounter++;
                            registeredClientsList.add(clientKey);
                        } catch(IOException e){
                            System.out.println(e.getMessage());
                        }
                    } else if (selectKey.isReadable()){
                        System.out.printf("%s is requesting to be read....%n", meta.getClientName());

                        /*
                            isReadable() just tells me that the OS is trying to queue some bytes that it is recieving.
                            reading it in a thread instead of immediately might mean the os has moved onto a different
                            procedure -> hence why it gives me 0.

                            call read() here and submit the worker task
                        */

                        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                        SocketChannel channel = (SocketChannel) selectKey.channel();
                        channel.configureBlocking(false);

                        int result = channel.read(byteBuffer);
                        String senderName = meta.getClientName();

                        //DEBUG START
                        if (result == -1) {
                            System.out.printf("Message from %s cannot be read as the remote channel is closed...", senderName);
                            continue;
                        } else if (result == 0) {
                            System.out.printf("No bytes were read from %s...", senderName);
                        }
                        //DEBUG END

                        byteBuffer.flip();
                        byte[] messageBytes = byteBuffer.array();

                        consumerThreadPool.submit(() -> consumerTask(messageBytes, selectKey, producerThreadPool));
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
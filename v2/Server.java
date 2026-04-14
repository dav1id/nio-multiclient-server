package v2;

/*
   First-Level: Loops through a list of selectionKeys, identifies the server selection channel and checks if a client
   is waiting to be accepted. Checks if other client channels are waiting to be read or written from. Generates a task
   and submits it to a ThreadPool as a ConsumerTask (reading) and a ProducerTask (writing).
*/

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private boolean status;
    public int clientCounter;

    public boolean consumerTask() {
        return true;
    }

    public boolean producerTask(){
        return true;
    }

    public void attachClientMeta(){

    };

    public void run(){
        status = true;
        try(
                ExecutorService producerThreadPool = Executors.newFixedThreadPool(3);
                ExecutorService consumerThreadPool = Executors.newFixedThreadPool(3);

                Selector selector = Selector.open();
                ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()){

            serverSocketChannel.bind(new InetSocketAddress(8080));
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            while(status){
                selector.select();

                Set<SelectionKey> selectionKeySet = selector.selectedKeys();
                Iterator<SelectionKey> selectedKeyIterator = selectionKeySet.iterator();

                while(selectedKeyIterator.hasNext()){
                    var selectKey = selectedKeyIterator.next();
                    selectedKeyIterator.remove();

                    if (selectKey.isAcceptable()){
                        try(SocketChannel client = serverSocketChannel.accept()){
                            client.configureBlocking(false);
                            client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_ACCEPT);
                            System.out.println("New client has been connected!");

                            selectKey.attach(new ClientMeta(client.getLocalAddress(), clientCounter));
                        }
                        // Accept a new socket channel connection here
                    } else if (selectKey.isReadable()){
                        producerThreadPool.submit( () -> {producerTask();} );
                    } else if (selectKey.isWritable()){
                        consumerThreadPool.submit( () -> {consumerTask();} );
                    }
                }
            }

        } catch(IOException e){
            System.out.println(e.getMessage());
        }
    }
}

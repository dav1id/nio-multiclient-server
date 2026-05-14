package main.java;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import main.Visuals.Controller;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;

public class Client extends Application implements Runnable {
    private final ArrayList<String> clientsList = new ArrayList<>();
    private final String senderName = "sender-thread";
    private final String receiverName = "receiver-thread";
    private final String[] threadPriority = {senderName, receiverName}; // set a thread priority for the receiver and not sender

    private static String messageContent;
    static boolean messageSet = false;

    private Stage applicationStage;
    public static void main(String[] args){
        launch(args);
    }


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


    public static void setMessageByController(String message){
        messageContent = message;
        messageSet = true;
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
            if (!messageSet) continue;

          //  if (messageContent == null) continue;

            try {
                writeBuffer.clear();

                String[] messageArray = messageContent.split(" ", 2);

                if (messageContent.equals("this close")) break;

                interactClientsList(false, null);

                if ((messageArray.length >= 2) && clientsList.contains(messageArray[0])) {
                    System.out.printf("Me -> %s: %s %n", messageArray[0], messageArray[1]);

                    byte[] messageBytes = messageContent.getBytes();

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

    public void start(Stage stage) throws Exception {
        applicationStage = stage;

        try(SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(8080))){
            ByteBuffer buff = ByteBuffer.allocate(16);
            if (socketChannel.read(buff) == 1) throw new IOException();
        } catch(IOException e){
            throw new Exception("Socket Error: Server socket is not operational....");
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/layout.fxml"));
        Parent root = loader.load();
        Controller controller = loader.getController();

        String css = this.getClass().getResource("/resources/application.css").toExternalForm();

        Scene mainScene = new Scene(root);
        mainScene.getStylesheets().add(css);

        stage.setResizable(false);

        applicationStage.setScene(mainScene);
        applicationStage.show();
    }
}
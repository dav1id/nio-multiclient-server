import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;

import java.io.OutputStream;
import java.io.PrintWriter;

import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class Server {
    private boolean status = false;
    HashMap<String, LinkedBlockingQueue<String>> blockingQueues = new HashMap<>();

    public void ProducerInputThread(String sender, InputStream inputStream) { // Need a way to exit out of while loop. Maybe program a way for the client to exit
        try (BufferedReader in = new BufferedReader(new InputStreamReader(inputStream))) {
            while (status) {
                String[] message;
                if (in.ready()) {
                    message = (in.readLine()).split(" ", 1);

                    String receiver = null;
                    for (String client : blockingQueues.keySet()) {
                        if (message[0].equals(client)) {
                            receiver = client;
                            blockingQueues.get(client).put(message[1]);
                        }
                    }

                    if (receiver != null) {
                        blockingQueues.get(receiver).put(message[2]);
                    } else {
                        String errorMessage = String.format("Cannot send %s, since %s does not exist", message[1], message[0]);
                        blockingQueues.get(sender).put(errorMessage); //Have a way to make errorMessages priority?
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     *
     **/
    public void ConsumerOutputThread(OutputStream outputStream, String key) {
        try (PrintWriter out = new PrintWriter(outputStream)) {
            LinkedBlockingQueue<String> queue = blockingQueues.get(key);
            while (status) {
                String message = queue.take();
                out.println(message);
            }
        } catch (InterruptedException f) {
            System.out.println(f.getMessage());
        }
    }

    ;


    public void run() {
        ArrayList<Socket> clientsList = new ArrayList<>();

        ExecutorService inputThreadPool = Executors.newFixedThreadPool(3);
        ExecutorService outputThreadPool = Executors.newFixedThreadPool(3);

        status = true;
        try {
            ServerSocket serverSocket = new ServerSocket(8080);
            Socket client = null;

            while (status) {
                client = serverSocket.accept();

                String clientName = String.format("Client%d", blockingQueues.size());
                blockingQueues.put(clientName, new LinkedBlockingQueue<>());

                InputStream inputStream = client.getInputStream();
                OutputStream outputStream = client.getOutputStream();

                inputThreadPool.submit(() -> {
                    ProducerInputThread(clientName, inputStream);
                });

                outputThreadPool.submit(() -> {
                    ConsumerOutputThread(outputStream, ("Client" + blockingQueues.size()));
                });

            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
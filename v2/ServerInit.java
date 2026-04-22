package v2;

public class ServerInit {
    public static void main(String[] args){
        new Thread(
                () -> {
                    Server server = new Server(3, 1024);
                    server.run();
                }
        ).start();

        //DEBUG START
        Client dummyClient = new Client();
        dummyClient.run();
        //DEBUG END
    }
}
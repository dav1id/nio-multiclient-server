package v2;

public class ServerInit {
    public static void main(String[] args){
        Client dummyClient = new Client();
        Server server = new Server();

        new Thread(dummyClient).start();
        server.run();
    }
}
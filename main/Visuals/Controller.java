package main.Visuals;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import main.java.Client;

/**
 Handles user clicks, and redirects input to the application, which redirects input to the Client
 **/
public class Controller {
    @FXML
    Button MessageSend;

    @FXML
    TextField MessageChat;

    @FXML
    public void initialize(){
        MessageSend.setOnAction(e -> {
            System.out.println("This button has been called!");
            String message = MessageChat.getText();
            System.out.println(message);

            Client.setMessageByController(message);
        });
    }
}


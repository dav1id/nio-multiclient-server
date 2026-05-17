package main.Visuals;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import main.java.MessageLock;

/**
 Handles user clicks, and redirects input to the application, which redirects input to the Client
 **/

public class Controller {
    @FXML
    Button MessageSend;

    @FXML
    TextField MessageChat;

    private MessageLock messageLock;

    public void setMessageLock(MessageLock messageLock){
        this.messageLock = messageLock;
    }
    @FXML
    public void initialize(){
        MessageSend.setOnAction(e -> {
            System.out.println("This button has been called!");
            String message = MessageChat.getText();

            messageLock.notify();
            System.out.println(message);
        });
    }
}


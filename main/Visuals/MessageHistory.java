package main.Visuals;

/*
    Keep a digital copy of the messageHistory by writing to a textFile. For now messageHistory is literally
    going to be written in a formatted string with \n. Keeping it as an ArrayList is going to force me to loop
    through them
*/
public class MessageHistory {
    private String messageHistory;
    private String path;

    private final boolean textFileExists = false;

    private boolean deserializeAsTextFile(String path) {
        return false;
    }

    /**
        Internal function that the public function destroyHistory() is going to call. Serializes
        the textFile into a directory called out.

        Client's are randomly assigned as this client server is designed to work on the device hosting
        the server in the first place... Need to add more functionality if multiple devices are going to
        be allowed to use it.
     **/
    private boolean serializeAsTextFile(String path) {
        return false;
        // Wrap with a try and catch
    }

    /**
        Calls deserializeAsTextFile() if the text file exists,
        Deserializing Message History if the text file exists -> Should be Host#To#.txt. So
        client1 sending a message to client2 will be 12.text. If it doesn't exist it's going to
        set textFileExists to be false and serialize the text file.

        For the purpose of testing textFileExists will always be false
     **/
    public MessageHistory() {

    }

    /**
        Serializing MessageHistory as a text file by calling the internal function serializeAsTextFile().
     **/
    public void destroy() {

    }

    /**
        Upload a string to MessageHistory
     **/
    public void setMessageHistory(String message) {

    }

    /**
        Return the messageHistory string
     **/
    public String getMessageHistory(String message) {
        return null;
    }
}
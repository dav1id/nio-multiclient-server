javac --module-path "$FX_PATH" \
--add-modules javafx.controls,javafx.fxml \
-d out \
$(find src -name "*.java")

java --module-path /Users/davidola/Downloads/javafx-sdk-21.0.11/lib \
--add-modules javafx.controls,javafx.fxml \
-cp out \
main.java.Client

#!/bin/bash

mkdir -p out

javac --module-path "$HOME/Downloads/javafx-sdk-21.0.11/lib" --add-modules javafx.controls,javafx.fxml -d out main/java/*.java main/Visuals/*.java resources/*.java

javac -d out main/java/*.java
javac -d out main/Visuals/*.java 
javac -d out resources/*.java

java -cp out main

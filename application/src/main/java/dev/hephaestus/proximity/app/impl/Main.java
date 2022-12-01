package dev.hephaestus.proximity.app.impl;

import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Path workingDirectory = Path.of(this.getParameters().getNamed().getOrDefault("working-directory", "."));
        Path pluginDirectory = Path.of(this.getParameters().getNamed().getOrDefault("plugin-directory", "plugins"));

        Proximity.init(workingDirectory, pluginDirectory);


        Parent root = FXMLLoader.load(getClass().getClassLoader().getResource("index.fxml"));
//
//        this.menuBar = new MenuBar(root, stage.getOwner());
//
//        root.getChildren().addAll(
//                this.menuBar,
//                this.content
//        );
//
//        this.content.getChildren().add(0, this.sidebar = new Sidebar());
//
//        this.content.getChildren().addAll(
//                this.dataEntryArea = new DataEntryArea(),
//                this.previewPane = new PreviewPane()
//        );
//
//        if (DATA_PROVIDER != null) {
//            this.menuBar.init(DATA_PROVIDER);
//        }
//
//        this.content.requestFocus();
//        VBox.setVgrow(this.content, Priority.ALWAYS);
//        HBox.setHgrow(this.dataEntryArea, Priority.ALWAYS);

        Scene scene = new Scene(root, 800, 480);

        scene.getStylesheets().add("style.css");

        stage.setTitle("Proximity");

        this.addIcons(stage);

        stage.setMinWidth(800);
        stage.setMinHeight(480);

        stage.setScene(scene);

        stage.setMaximized(true);

        stage.show();
    }

    private void addIcons(Stage stage) {
        ObservableList<Image> icons = stage.getIcons();

        for (int i = 16; i <= 2048; i *= 2) {
            try {
                InputStream stream = getClass().getModule().getResourceAsStream("icons/icon" + i + ".png");

                if (stream != null) {
                    icons.add(new Image(stream));
                }
            } catch (IOException ignored) {

            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

package de.hhu.stups.plues.ui;

import com.google.inject.Guice;
import com.google.inject.Injector;
import de.hhu.stups.plues.injector.PluesModule;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

public class Main extends Application {


    @Override
    public void start(Stage primaryStage) throws Exception {
        Injector injector = Guice.createInjector(com.google.inject.Stage.DEVELOPMENT, new PluesModule());

        FXMLLoader loader = injector.getInstance(FXMLLoader.class);

        URL main = getClass().getResource("/fxml/main.fxml");
        loader.setLocation(main);

        Parent root = loader.load();

        primaryStage.setTitle("Hello World");
        primaryStage.setScene(new Scene(root, 800, 600));

        // TODO: properly close the application on close request
        Platform.setImplicitExit(true);
        primaryStage.setOnCloseRequest(t -> Platform.exit());

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
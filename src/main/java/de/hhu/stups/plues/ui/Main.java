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


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Injector injector = Guice.createInjector(
                com.google.inject.Stage.DEVELOPMENT, new PluesModule(primaryStage));

        Router router = injector.getInstance(Router.class);

        router.transitionTo("index");

        primaryStage.setTitle("PlÜS");

        // TODO: properly close the application on close request
        Platform.setImplicitExit(true);
        primaryStage.setOnCloseRequest(t -> Platform.exit());

        primaryStage.show();
    }
}

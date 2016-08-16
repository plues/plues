package de.hhu.stups.plues.ui;

import static com.google.inject.Stage.DEVELOPMENT;

import com.google.inject.Guice;
import com.google.inject.Injector;

import de.hhu.stups.plues.injector.PluesModule;
import de.hhu.stups.plues.routes.Router;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;


public class Main extends Application {


  public static void main(final String[] args) {
    launch(args);
  }

  @Override
  public void start(final Stage primaryStage) throws Exception {
    final Injector injector = Guice.createInjector(DEVELOPMENT, new PluesModule(primaryStage));

    final Router router = injector.getInstance(Router.class);
    final ResourceManager rm = injector.getInstance(ResourceManager.class);
    router.transitionTo("index");

    primaryStage.setTitle("PlÜS");

    Platform.setImplicitExit(true);

    primaryStage.setOnCloseRequest(t -> {
      try {
        rm.close();
      } catch (final InterruptedException exception) {
        exception.printStackTrace();
      }
      Platform.exit();
    });

    primaryStage.show();
  }
}

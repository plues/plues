package de.hhu.stups.plues.ui;

import static com.google.inject.Stage.DEVELOPMENT;

import com.google.inject.Guice;
import com.google.inject.Injector;

import de.hhu.stups.plues.injector.PluesModule;
import de.hhu.stups.plues.routes.RouteNames;
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

    // XXX load an instance of Main.class to ensure Prob 2.0 is properly loaded.
    // Among other things this sets prob.home to load files from the ProB stdlib.
    injector.getInstance(de.prob.Main.class);

    final Router router = injector.getInstance(Router.class);
    router.transitionTo(RouteNames.INDEX);

    primaryStage.setTitle("PlÜS");

    Platform.setImplicitExit(true);


    primaryStage.show();
  }
}

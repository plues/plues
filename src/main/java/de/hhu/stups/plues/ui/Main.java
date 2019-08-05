package de.hhu.stups.plues.ui;

import com.google.inject.Guice;
import com.google.inject.Injector;
import de.hhu.stups.plues.injector.PluesModule;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.routes.Router;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javafx.application.Application;
import javafx.stage.Stage;

import static com.google.inject.Stage.PRODUCTION;


public class Main extends Application {

  @Override
  @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
  public void start(final Stage primaryStage) throws Exception {
    System.setProperty("logback.configurationFile", "config/logging.xml");

    final Injector injector = Guice.createInjector(PRODUCTION, new PluesModule(primaryStage));

    // XXX load an instance of Main.class to ensure Prob 2.0 is properly loaded.
    // Among other things this sets prob.home to load files from the ProB stdlib.
    injector.getInstance(de.prob.Main.class);

    final Router router = injector.getInstance(Router.class);
    //noinspection unused
    final ResourceManager resourceManager = injector.getInstance(ResourceManager.class);

    router.transitionTo(RouteNames.INDEX);

    primaryStage.setTitle("PlÜS");

    primaryStage.show();
  }
}

package de.hhu.stups.plues.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.hhu.stups.plues.routes.ControllerRoute;
import de.hhu.stups.plues.routes.IndexRoute;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.stage.Stage;

public class RouterProvider implements Provider<Router> {

  private final Inflater inflater;
  private final Stage stage;
  private Router cache;

  @Inject
  public RouterProvider(final Inflater inflater, final Stage stage) {
    this.inflater = inflater;
    this.stage = stage;
  }

  @Override
  public Router get() {
    if (cache == null) {
      cache = new Router();

      cache.put("index", new IndexRoute(inflater, stage));
      cache.put("timetableView", new ControllerRoute(stage, 0));
      cache.put("pdfTimetables", new ControllerRoute(stage, 1));
      cache.put("partialTimetables", new ControllerRoute(stage, 2));
      cache.put("unsatCore", new ControllerRoute(stage, 5));
    }

    return cache;
  }
}

package de.hhu.stups.plues.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.hhu.stups.plues.routes.ControllerRouteFactory;
import de.hhu.stups.plues.routes.IndexRoute;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.routes.Router;

public class RouterProvider implements Provider<Router> {

  private final Provider<IndexRoute> indexRouteProvider;
  private final ControllerRouteFactory controllerRouteFactory;
  private Router cache;

  /**
   * The {@link Router} provider.
   */
  @Inject
  public RouterProvider(final Provider<IndexRoute> indexRouteProvider,
                        final ControllerRouteFactory controllerRouteFactory) {
    this.indexRouteProvider = indexRouteProvider;
    this.controllerRouteFactory = controllerRouteFactory;
  }

  @Override
  public Router get() {
    if (cache == null) {
      cache = new Router();

      cache.put(RouteNames.INDEX.getRouteName(), indexRouteProvider.get());
      cache.put(RouteNames.TIMETABLE.getRouteName(),
          controllerRouteFactory.create("tabTimetable"));
      cache.put(RouteNames.PDF_TIMETABLES.getRouteName(),
          controllerRouteFactory.create("tabPdfTimetables"));
      cache.put(RouteNames.PARTIAL_TIMETABLES.getRouteName(),
          controllerRouteFactory.create("tabPartialTimetables"));
      cache.put(RouteNames.UNSAT_CORE.getRouteName(),
          controllerRouteFactory.create("tabUnsatCore"));
    }

    return cache;
  }
}

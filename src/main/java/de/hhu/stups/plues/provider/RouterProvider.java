package de.hhu.stups.plues.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.hhu.stups.plues.routes.AbstractUnitDetailViewRoute;
import de.hhu.stups.plues.routes.CourseDetailViewRoute;
import de.hhu.stups.plues.routes.IndexRoute;
import de.hhu.stups.plues.routes.ModuleDetailViewRoute;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.routes.SessionDetailViewRoute;
import de.hhu.stups.plues.routes.UnitDetailViewRoute;

public class RouterProvider implements Provider<Router> {
  private final Provider<IndexRoute> indexRouteProvider;
  private Router cache;

  private final Provider<ModuleDetailViewRoute> moduleDetailViewRouteProvider;
  private final Provider<AbstractUnitDetailViewRoute> abstractUnitDetailViewRouteProvider;
  private final Provider<UnitDetailViewRoute> unitDetailViewRouteProvider;
  private final Provider<SessionDetailViewRoute> sessionDetailViewRouteProvider;
  private final Provider<CourseDetailViewRoute> courseDetailViewRouteProvider;

  /**
   * Constructor for routerProvider.
   */
  @Inject
  public RouterProvider(final Provider<IndexRoute> indexRouteProvider,
                        final Provider<ModuleDetailViewRoute> moduleDetailViewRouteProvider,
                        final Provider<AbstractUnitDetailViewRoute>
                            abstractUnitDetailViewRouteProvider,
                        final Provider<UnitDetailViewRoute> unitDetailViewRouteProvider,
                        final Provider<SessionDetailViewRoute> sessionDetailViewRouteProvider,
                        final Provider<CourseDetailViewRoute> courseDetailViewRouteProvider) {
    this.indexRouteProvider = indexRouteProvider;
    this.moduleDetailViewRouteProvider = moduleDetailViewRouteProvider;
    this.abstractUnitDetailViewRouteProvider = abstractUnitDetailViewRouteProvider;
    this.unitDetailViewRouteProvider = unitDetailViewRouteProvider;
    this.sessionDetailViewRouteProvider = sessionDetailViewRouteProvider;
    this.courseDetailViewRouteProvider = courseDetailViewRouteProvider;
  }

  @Override
  public Router get() {
    if (cache == null) {
      cache = new Router();

      cache.put(RouteNames.INDEX.getRouteName(), indexRouteProvider.get());
      cache.put(RouteNames.MODULE_DETAIL_VIEW.getRouteName(), moduleDetailViewRouteProvider.get());
      cache.put(RouteNames.SESSION_DETAIL_VIEW.getRouteName(), sessionDetailViewRouteProvider.get());
      cache.put(RouteNames.ABSTRACT_UNIT_DETAIL_VIEW.getRouteName(),
          abstractUnitDetailViewRouteProvider.get());
      cache.put(RouteNames.UNIT_DETAIL_VIEW.getRouteName(), unitDetailViewRouteProvider.get());
      cache.put(RouteNames.COURSE.getRouteName(), courseDetailViewRouteProvider.get());
    }

    return cache;
  }
}

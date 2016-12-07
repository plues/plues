package de.hhu.stups.plues.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.hhu.stups.plues.routes.AbstractUnitDetailViewRoute;
import de.hhu.stups.plues.routes.IndexRoute;
import de.hhu.stups.plues.routes.ModuleDetailViewRoute;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.routes.SessionDetailViewRoute;
import de.hhu.stups.plues.routes.UnitDetailViewRoute;

public class RouterProvider implements Provider<Router> {
  private final Provider<IndexRoute> indexRouteProvider;
  private Router cache;

  private final Provider<ModuleDetailViewRoute> moduleDetailViewProvider;
  private Provider<AbstractUnitDetailViewRoute> abstractUnitDetailViewProvider;
  private Provider<UnitDetailViewRoute> unitDetailViewProvider;
  private Provider<SessionDetailViewRoute> sessionDetailViewProvider;

  /**
   * Constructor for routerProvider.
   */
  @Inject
  public RouterProvider(final Provider<IndexRoute> indexRouteProvider,
                        final Provider<ModuleDetailViewRoute> moduleDetailViewRouteProvider,
                        final Provider<AbstractUnitDetailViewRoute>
                            abstractUnitDetailViewRouteProvider,
                        final Provider<UnitDetailViewRoute> unitDetailViewRouteProvider,
                        final Provider<SessionDetailViewRoute> sessionDetailViewRouteProvider) {
    this.indexRouteProvider = indexRouteProvider;
    this.moduleDetailViewProvider = moduleDetailViewRouteProvider;
    this.abstractUnitDetailViewProvider = abstractUnitDetailViewRouteProvider;
    this.unitDetailViewProvider = unitDetailViewRouteProvider;
    this.sessionDetailViewProvider = sessionDetailViewRouteProvider;
  }

  @Override
  public Router get() {
    if (cache == null) {
      cache = new Router();

      cache.put(RouteNames.INDEX.getRouteName(), indexRouteProvider.get());
      cache.put(RouteNames.MODULE_DETAIL_VIEW.getRouteName(), moduleDetailViewProvider.get());
      cache.put(RouteNames.SESSION_DETAIL_VIEW.getRouteName(), sessionDetailViewProvider.get());
      cache.put(RouteNames.ABSTRACT_UNIT_DETAIL_VIEW.getRouteName(),
          abstractUnitDetailViewProvider.get());
      cache.put(RouteNames.UNIT_DETAIL_VIEW.getRouteName(), unitDetailViewProvider.get());
    }

    return cache;
  }
}

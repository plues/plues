package de.hhu.stups.plues.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.hhu.stups.plues.routes.AboutWindowRoute;
import de.hhu.stups.plues.routes.AbstractUnitDetailViewRoute;
import de.hhu.stups.plues.routes.ChangelogRoute;
import de.hhu.stups.plues.routes.ControllerRouteFactory;
import de.hhu.stups.plues.routes.CourseDetailViewRoute;
import de.hhu.stups.plues.routes.HandbookRoute;
import de.hhu.stups.plues.routes.IndexRoute;
import de.hhu.stups.plues.routes.ModuleDetailViewRoute;
import de.hhu.stups.plues.routes.ReportsRoute;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.routes.SessionDetailViewRoute;
import de.hhu.stups.plues.routes.UnitDetailViewRoute;

public class RouterProvider implements Provider<Router> {

  private final ControllerRouteFactory controllerRouteFactory;
  private final Provider<HandbookRoute> handbookRouterProvider;
  private Router cache;

  private final Provider<ChangelogRoute> changelogRouteProvider;
  private final Provider<AboutWindowRoute> aboutWindowRouteProvider;
  private final Provider<ModuleDetailViewRoute> moduleDetailViewRouteProvider;
  private final Provider<AbstractUnitDetailViewRoute> abstractUnitDetailViewRouteProvider;
  private final Provider<UnitDetailViewRoute> unitDetailViewRouteProvider;
  private final Provider<SessionDetailViewRoute> sessionDetailViewRouteProvider;
  private final Provider<CourseDetailViewRoute> courseDetailViewRouteProvider;
  private final Provider<ReportsRoute> reportsRouteProvider;
  private final Provider<IndexRoute> indexRouteProvider;

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
                        final Provider<CourseDetailViewRoute> courseDetailViewRouteProvider,
                        final Provider<AboutWindowRoute> aboutWindowRouteProvider,
                        final Provider<ReportsRoute> reportsRouteProvider,
                        final Provider<ChangelogRoute> changelogRouteProvider,
                        final ControllerRouteFactory controllerRouteFactory,
                        final Provider<HandbookRoute> handbookRouteProvider) {
    this.indexRouteProvider = indexRouteProvider;
    this.moduleDetailViewRouteProvider = moduleDetailViewRouteProvider;
    this.abstractUnitDetailViewRouteProvider = abstractUnitDetailViewRouteProvider;
    this.unitDetailViewRouteProvider = unitDetailViewRouteProvider;
    this.sessionDetailViewRouteProvider = sessionDetailViewRouteProvider;
    this.courseDetailViewRouteProvider = courseDetailViewRouteProvider;
    this.aboutWindowRouteProvider = aboutWindowRouteProvider;
    this.reportsRouteProvider = reportsRouteProvider;
    this.changelogRouteProvider = changelogRouteProvider;
    this.controllerRouteFactory = controllerRouteFactory;
    this.handbookRouterProvider = handbookRouteProvider;
  }

  @Override
  public Router get() {
    if (cache == null) {
      cache = new Router();

      cache.put(RouteNames.INDEX, indexRouteProvider.get());
      cache.put(RouteNames.MODULE_DETAIL_VIEW, moduleDetailViewRouteProvider.get());
      cache.put(RouteNames.SESSION_DETAIL_VIEW,
          sessionDetailViewRouteProvider.get());
      cache.put(RouteNames.ABSTRACT_UNIT_DETAIL_VIEW,
          abstractUnitDetailViewRouteProvider.get());
      cache.put(RouteNames.UNIT_DETAIL_VIEW, unitDetailViewRouteProvider.get());
      cache.put(RouteNames.COURSE_DETAIL_VIEW, courseDetailViewRouteProvider.get());
      cache.put(RouteNames.ABOUT_WINDOW, aboutWindowRouteProvider.get());
      cache.put(RouteNames.REPORTS, reportsRouteProvider.get());
      cache.put(RouteNames.CHANGELOG, changelogRouteProvider.get());
      cache.put(RouteNames.HANDBOOK, handbookRouterProvider.get());
      cache.put(RouteNames.TIMETABLE,
          controllerRouteFactory.create("tabTimetable"));
      cache.put(RouteNames.PDF_TIMETABLES,
          controllerRouteFactory.create("tabPdfTimetables"));
      cache.put(RouteNames.PARTIAL_TIMETABLES,
          controllerRouteFactory.create("tabPartialTimetables"));
      cache.put(RouteNames.UNSAT_CORE,
          controllerRouteFactory.create("tabUnsatCore"));
    }

    return cache;
  }
}

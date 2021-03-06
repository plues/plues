package de.hhu.stups.plues.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import de.hhu.stups.plues.routes.AboutWindowRoute;
import de.hhu.stups.plues.routes.ChangelogRoute;
import de.hhu.stups.plues.routes.ControllerRouteFactory;
import de.hhu.stups.plues.routes.DetailViewRoute;
import de.hhu.stups.plues.routes.HandbookRoute;
import de.hhu.stups.plues.routes.HandbookRouteFactory;
import de.hhu.stups.plues.routes.IndexRoute;
import de.hhu.stups.plues.routes.MainControllerRoute;
import de.hhu.stups.plues.routes.RouteNames;
import de.hhu.stups.plues.routes.Router;
import javafx.application.Platform;

@Singleton
public class RouterProvider implements Provider<Router> {

  private static final String TAB_TIMETABLE = "tabTimetable";

  private final ControllerRouteFactory controllerRouteFactory;
  private final DetailViewRoute detailViewRoute;
  private final HandbookRouteFactory handbookRouteFactory;
  private Router router;

  private final Provider<MainControllerRoute> mainControllerRouteProvider;
  private final Provider<ChangelogRoute> changelogRouteProvider;
  private final Provider<AboutWindowRoute> aboutWindowRouteProvider;
  private final Provider<IndexRoute> indexRouteProvider;

  /**
   * Constructor for routerProvider.
   */
  @Inject
  public RouterProvider(final Provider<IndexRoute> indexRouteProvider,
                        final Provider<AboutWindowRoute> aboutWindowRouteProvider,
                        final Provider<ChangelogRoute> changelogRouteProvider,
                        final Provider<MainControllerRoute> mainControllerRouteProvider,
                        final ControllerRouteFactory controllerRouteFactory,
                        final DetailViewRoute detailViewRoute,
                        final HandbookRouteFactory handbookRouteFactory) {
    this.indexRouteProvider = indexRouteProvider;
    this.aboutWindowRouteProvider = aboutWindowRouteProvider;
    this.changelogRouteProvider = changelogRouteProvider;
    this.mainControllerRouteProvider = mainControllerRouteProvider;
    this.controllerRouteFactory = controllerRouteFactory;
    this.detailViewRoute = detailViewRoute;
    this.handbookRouteFactory = handbookRouteFactory;
  }

  @Override
  public Router get() {
    if (router == null) {
      router = new Router();

      router.register(RouteNames.INDEX, indexRouteProvider.get());
      //
      router.register(RouteNames.MODULE_DETAIL_VIEW, detailViewRoute);
      router.register(RouteNames.SESSION_DETAIL_VIEW, detailViewRoute);
      router.register(RouteNames.ABSTRACT_UNIT_DETAIL_VIEW, detailViewRoute);
      router.register(RouteNames.UNIT_DETAIL_VIEW, detailViewRoute);
      router.register(RouteNames.COURSE_DETAIL_VIEW, detailViewRoute);
      //
      router.register(RouteNames.ABOUT_WINDOW, aboutWindowRouteProvider.get());
      router.register(RouteNames.CHANGELOG, changelogRouteProvider.get());
      router.register(RouteNames.HANDBOOK_HTML,
          handbookRouteFactory.create(HandbookRoute.Format.HTML));
      router.register(RouteNames.HANDBOOK_PDF,
          handbookRouteFactory.create(HandbookRoute.Format.PDF));
      //
      router.register(RouteNames.TIMETABLE,
          controllerRouteFactory.create(TAB_TIMETABLE));
      router.register(RouteNames.SESSION_IN_TIMETABLE,
          controllerRouteFactory.create(TAB_TIMETABLE));
      router.register(RouteNames.CONFLICT_IN_TIMETABLE,
          controllerRouteFactory.create(TAB_TIMETABLE));
      router.register(RouteNames.CHECK_FEASIBILITY_TIMETABLE,
          controllerRouteFactory.create(TAB_TIMETABLE));
      router.register(RouteNames.PDF_TIMETABLES,
          controllerRouteFactory.create("tabPdfTimetables"));
      router.register(RouteNames.PARTIAL_TIMETABLES,
          controllerRouteFactory.create("tabPartialTimetables"));
      router.register(RouteNames.UNSAT_CORE,
          controllerRouteFactory.create("tabUnsatCore"));
      //
      router.register(RouteNames.OPEN_REPORTS, mainControllerRouteProvider.get());
      //
      router.register(RouteNames.CLOSE_APP, mainControllerRouteProvider.get());
      router.register(RouteNames.SHUTDOWN, (routeName, args) -> Platform.exit());
    }

    return router;
  }
}

package de.hhu.stups.plues.injector;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

import de.hhu.stups.plues.provider.RouterProvider;
import de.hhu.stups.plues.routes.AboutWindowRoute;
import de.hhu.stups.plues.routes.ControllerRouteFactory;
import de.hhu.stups.plues.routes.DetailViewRoute;
import de.hhu.stups.plues.routes.HandbookRouteFactory;
import de.hhu.stups.plues.routes.IndexRoute;
import de.hhu.stups.plues.routes.MainControllerRoute;
import de.hhu.stups.plues.routes.Router;

public class RouterModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(Router.class).toProvider(RouterProvider.class);
    bind(AboutWindowRoute.class);
    bind(DetailViewRoute.class);
    bind(IndexRoute.class);
    bind(MainControllerRoute.class);

    install(new FactoryModuleBuilder().build(ControllerRouteFactory.class));
    install(new FactoryModuleBuilder().build(HandbookRouteFactory.class));

  }
}

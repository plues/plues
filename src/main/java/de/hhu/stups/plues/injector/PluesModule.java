package de.hhu.stups.plues.injector;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.prob.MockSolver;
import de.hhu.stups.plues.prob.ProBSolver;
import de.hhu.stups.plues.prob.Solver;
import de.hhu.stups.plues.prob.SolverFactory;
import de.hhu.stups.plues.provider.RouterProvider;
import de.hhu.stups.plues.tasks.SolverLoaderTaskFactory;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.ui.components.ResultBoxFactory;
import de.hhu.stups.plues.ui.controller.MainController;
import de.prob.MainModule;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

public class PluesModule extends AbstractModule {

    private final TypeLiteral<Delayed<Store>> delayedStoreType
            = new TypeLiteral<Delayed<Store>>() {
    };

    private final TypeLiteral<Delayed<SolverService>> delayedSolverServiceType
            = new TypeLiteral<Delayed<SolverService>>() {
    };

    private final Stage primaryStage;

    public PluesModule(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    @Override
    public final void configure() {
        // prob 2.0
        install(new MainModule());

        install(new PropertiesModule());

        install(new FactoryModuleBuilder()
                        .build(SolverLoaderTaskFactory.class));
        install(new FactoryModuleBuilder()
                        .implement(Solver.class, Names.named("prob"), ProBSolver.class)
                        .implement(Solver.class, Names.named("mock"), MockSolver.class)
                        .build(SolverFactory.class));
        install(new FactoryModuleBuilder().build(ResultBoxFactory.class));

        bind(Stage.class).toInstance(primaryStage);
        bind(Router.class).toProvider(RouterProvider.class);

        bind(MainController.class);
        install(new ComponentsModule());

        bind(delayedStoreType).toInstance(new Delayed<>());
        bind(delayedSolverServiceType).toInstance(new Delayed<>());
    }

    @Provides
    public final FXMLLoader provideLoader(final Injector injector,
                                          final GuiceBuilderFactory
                                                  builderFactory) {

        final FXMLLoader fxmlLoader = new FXMLLoader();

        fxmlLoader.setBuilderFactory(builderFactory);
        fxmlLoader.setControllerFactory(injector::getInstance);

        return fxmlLoader;
    }

}

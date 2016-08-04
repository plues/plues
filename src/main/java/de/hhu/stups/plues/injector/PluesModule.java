package de.hhu.stups.plues.injector;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.prob.SolverFactory;
import de.hhu.stups.plues.tasks.SolverLoaderTaskFactory;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.ui.controller.CourseFilter;
import de.hhu.stups.plues.ui.controller.Musterstudienplaene;
import de.prob.MainModule;
import javafx.fxml.FXMLLoader;

public class PluesModule extends AbstractModule {

    private final TypeLiteral<Delayed<Store>> delayedStoreType
            = new TypeLiteral<Delayed<Store>>() {
    };

    private final TypeLiteral<Delayed<SolverService>> delayedSolverServiceType
            = new TypeLiteral<Delayed<SolverService>>() {
    };

    @Override
    public final void configure() {
        // prob 2.0
        install(new MainModule());

        install(new PropertiesModule());

        install(new FactoryModuleBuilder()
                .build(SolverLoaderTaskFactory.class));
        install(new FactoryModuleBuilder().build(SolverFactory.class));

        bind(CourseFilter.class);
        bind(Musterstudienplaene.class);

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

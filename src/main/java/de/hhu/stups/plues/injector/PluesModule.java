package de.hhu.stups.plues.injector;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.tasks.SolverLoaderTaskFactory;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.ui.controller.CourseFilter;
import de.hhu.stups.plues.ui.controller.Musterstudienplaene;
import de.prob.MainModule;
import javafx.fxml.FXMLLoader;

public class PluesModule extends AbstractModule {

    @Override
    public void configure() {
        // prob 2.0
        install(new MainModule());

        install(new PropertiesModule());

        install(new FactoryModuleBuilder()
                .build(SolverLoaderTaskFactory.class));

        bind(CourseFilter.class);
        bind(Musterstudienplaene.class);

        bind(new TypeLiteral<Delayed<Store>>() {}).toInstance(new Delayed<>());
        bind(new TypeLiteral<Delayed<SolverService>>() {}).toInstance(new Delayed<>());
    }

    @Provides
    public FXMLLoader provideLoader(Injector injector, GuiceBuilderFactory builderFactory) {
        FXMLLoader fxmlLoader = new FXMLLoader();

        fxmlLoader.setBuilderFactory(builderFactory);
        fxmlLoader.setControllerFactory(injector::getInstance);

        return fxmlLoader;
    }

}

package de.hhu.stups.plues.injector;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import de.hhu.stups.plues.data.AbstractStore;
import de.hhu.stups.plues.tasks.SolverLoaderTaskFactory;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.ui.controller.CourseFilter;
import de.prob.MainModule;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
        bind(EventBus.class).toInstance(new EventBus());
        bind(SolverService.class);

        bind(new TypeLiteral<ObjectProperty<AbstractStore>>() {}).toInstance(new SimpleObjectProperty<>());
        bind(new TypeLiteral<ObjectProperty<de.hhu.stups.plues.prob.Solver>>() {}).toInstance(new SimpleObjectProperty<>());
    }

    @Provides
    public FXMLLoader provideLoader(Injector injector, GuiceBuilderFactory builderFactory) {
        FXMLLoader fxmlLoader = new FXMLLoader();

        fxmlLoader.setBuilderFactory(builderFactory);
        fxmlLoader.setControllerFactory(injector::getInstance);

        return fxmlLoader;
    }

}

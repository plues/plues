package de.hhu.stups.plues.injector;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import de.hhu.stups.plues.data.AbstractStore;
import de.hhu.stups.plues.ui.controller.CourseFilter;
import de.prob.MainModule;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static java.lang.Thread.currentThread;

public class PluesModule extends AbstractModule {
    @Override
    public void configure() {
        // prob 2.0
        install(new MainModule());

        bind(CourseFilter.class);
        bind(EventBus.class).toInstance(new EventBus());

        bind(ObjectProperty.class).annotatedWith(Store.class).toInstance(new SimpleObjectProperty<AbstractStore>());
        bind(ObjectProperty.class).annotatedWith(Solver.class).toInstance(new SimpleObjectProperty<de.hhu.stups.plues.prob.Solver>());

    }

    @Provides
    public FXMLLoader provideLoader(Injector injector, GuiceBuilderFactory builderFactory) {
        FXMLLoader fxmlLoader = new FXMLLoader();

        fxmlLoader.setBuilderFactory(builderFactory);
        fxmlLoader.setControllerFactory(injector::getInstance);

        return fxmlLoader;
    }

    @Provides
    public Properties provideProperties() {
        return setupProperties();
    }

    private static Properties setupProperties() {
        Properties defaults = new Properties();
        Properties properties = loadProperties(defaults, "main", "local");

        // settings that can be overriden in env vars
        String[] env = new String[]{"MODELPATH", "DBPATH"};

        for (String it : env) {
            if (System.getenv(it) != null) {
                properties.setProperty(it.toLowerCase(), System.getenv(it));
            }
        }
        return properties;
    }

    private static Properties loadProperties(Properties properties, String... propertyFiles) {
        for (String propertyFile : propertyFiles) {
            try {
                InputStream p = currentThread().getContextClassLoader().getResourceAsStream(propertyFile + ".properties");
                if (p == null) {
                    continue;
                }
                properties.load(p);
            } catch (FileNotFoundException e) {
                System.err.println(propertyFile + ".properties is missing!");
            } catch (IOException e) {
                System.err.println(propertyFile + ".properties produced IO Error!");
            }
        }
        return properties;
    }
}

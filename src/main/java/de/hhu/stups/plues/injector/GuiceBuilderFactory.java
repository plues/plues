package de.hhu.stups.plues.injector;

import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.util.Builder;
import javafx.util.BuilderFactory;

public class GuiceBuilderFactory implements BuilderFactory {

    BuilderFactory javafxDefaultBuilderFactory = new JavaFXBuilderFactory();
    private Injector injector;

    @Inject
    public GuiceBuilderFactory(Injector injector) {
        this.injector = injector;
    }

    @Override
    public Builder<?> getBuilder(Class<?> type) {
        if (isGuiceResponsibleForType(type)) {
            Object instance = injector.getInstance(type);
            return wrapInstanceInBuilder(instance);
        }
        return javafxDefaultBuilderFactory.getBuilder(type);
    }

    private Builder<?> wrapInstanceInBuilder(Object instance) {
        return () -> instance;
    }

    private boolean isGuiceResponsibleForType(Class<?> type) {
        Binding<?> binding = injector.getExistingBinding(Key.get(type));
        return binding != null;
    }

}
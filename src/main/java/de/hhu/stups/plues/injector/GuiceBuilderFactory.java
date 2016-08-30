package de.hhu.stups.plues.injector;

import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;

import javafx.fxml.JavaFXBuilderFactory;
import javafx.util.Builder;
import javafx.util.BuilderFactory;

class GuiceBuilderFactory implements BuilderFactory {

  private final BuilderFactory javafxDefaultBuilderFactory = new JavaFXBuilderFactory();
  private final Injector injector;

  @Inject
  public GuiceBuilderFactory(final Injector injector) {
    this.injector = injector;
  }

  @Override
  public Builder<?> getBuilder(final Class<?> type) {
    if (isGuiceResponsibleForType(type)) {
      final Object instance = injector.getInstance(type);
      return wrapInstanceInBuilder(instance);
    }
    return javafxDefaultBuilderFactory.getBuilder(type);
  }

  private Builder<?> wrapInstanceInBuilder(final Object instance) {
    return () -> instance;
  }

  private boolean isGuiceResponsibleForType(final Class<?> type) {
    final Binding<?> binding = injector.getExistingBinding(Key.get(type));
    return binding != null;
  }

}

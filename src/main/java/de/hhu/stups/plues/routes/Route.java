package de.hhu.stups.plues.routes;

/**
 * Interface for defining a transition between {@link javafx.scene.Scene}.
 */
@FunctionalInterface
interface Route {
  void transition(RouteNames routeName, Object... args);
}

package de.hhu.stups.plues.routes;

import com.google.inject.Singleton;

import java.util.HashMap;

/**
 * Allows transitions from scene to scene via a {@link Route}.
 */
@Singleton
public class Router extends HashMap<RouteNames, Route> {
  public void transitionTo(final RouteNames routeName, final Object... args) {
    this.get(routeName).transition(args);
  }
}

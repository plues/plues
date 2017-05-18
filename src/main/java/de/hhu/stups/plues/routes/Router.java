package de.hhu.stups.plues.routes;

import com.google.inject.Singleton;

import org.reactfx.EventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Allows transitions from scene to scene via a {@link Route}.
 */
@Singleton
public class Router {
  private final EventSource<NavigationEvent> eventSource;
  private final Logger logger = LoggerFactory.getLogger(getClass());


  /**
   * Create a new Router instance.
   */
  public Router() {
    this.eventSource = new EventSource<>();
    this.eventSource.subscribe(navigationEvent
        -> logger.debug(String.format("Navigating to %s", navigationEvent)));
  }

  /**
   * Register a new eventHandler for a specific route.
   * @param routeName RouteNames
   * @param route Route
   */
  public void register(final RouteNames routeName, final Route route) {
    eventSource
        .filter(navigationEvent -> navigationEvent.getRouteName().equals(routeName))
        .subscribe(navigationEvent
            -> route.transition(navigationEvent.routeName, navigationEvent.args));
  }

  public void transitionTo(final RouteNames routeName, final Object... args) {
    this.eventSource.push(new NavigationEvent(routeName, args));
  }

  private class NavigationEvent {

    private final RouteNames routeName;
    private final Object[] args;

    NavigationEvent(final RouteNames routeName, final Object[] args) {
      this.routeName = routeName;
      this.args = args;
    }

    private RouteNames getRouteName() {
      return routeName;
    }

    public String toString() {
      return String.format("Route: %s - args: %s", routeName.name(),
          Arrays.stream(args).map(Object::toString).collect(Collectors.toList()));
    }
  }
}

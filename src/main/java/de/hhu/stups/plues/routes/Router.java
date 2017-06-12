package de.hhu.stups.plues.routes;

import com.google.inject.Singleton;

import org.reactfx.EventSource;
import org.reactfx.EventStream;
import org.reactfx.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Allows transitions from scene to scene via a {@link Route}.
 */
@Singleton
public class Router {

  private final EventSource<NavigationEvent> eventSource;
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final HashMap<Integer, Subscription> subscriptions;
  private final AtomicInteger counter = new AtomicInteger(0);
  private final EventStream<NavigationEvent> eventStream;

  /**
   * Create a new Router instance.
   */
  public Router() {
    this.eventSource = new EventSource<>();
    this.eventStream = eventSource.onRecurseQueue();

    this.subscriptions = new HashMap<>();
    this.eventSource.subscribe(navigationEvent
        -> logger.debug(String.format("Navigating to %s", navigationEvent)));
  }

  /**
   * Register a new eventHandler for a specific route.
   * @param routeName RouteNames
   * @param route Route
   */
  public Integer register(final RouteNames routeName, final Route route) {
    if (logger.isDebugEnabled()) {
      logger.debug(String.format("Registering consumer for route %s ", routeName.name()));
    }

    final Integer idx = counter.getAndIncrement();

    final Predicate<NavigationEvent> navigationEventPredicate
        = navigationEvent -> navigationEvent.getRouteName().equals(routeName);
    final Consumer<NavigationEvent> navigationEventConsumer
        = navigationEvent -> route.transition(navigationEvent.routeName, navigationEvent.args);

    final Subscription subscription
        = eventStream.filter(navigationEventPredicate).subscribe(navigationEventConsumer);

    subscriptions.put(idx, subscription);
    return idx;
  }

  public void transitionTo(final RouteNames routeName, final Object... args) {
    this.eventSource.push(new NavigationEvent(routeName, args));
  }

  /**
   * Remove a registered route handler. Handlers are identified by their ID as returned when
   * registering the handler.
   * @param routeId Integer
   * @throws IllegalArgumentException if routeId is invalid
   */
  @SuppressWarnings("WeakerAccess")
  public void deregister(final Integer routeId) {
    if (!this.subscriptions.containsKey(routeId)) {
      throw new IllegalArgumentException(String.format("Unknown route id %s", routeId));
    }
    logger.debug("Removing route consumer");
    this.subscriptions.remove(routeId).unsubscribe();
  }

  private static class NavigationEvent {

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

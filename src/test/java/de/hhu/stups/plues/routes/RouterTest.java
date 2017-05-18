package de.hhu.stups.plues.routes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

public class RouterTest {

  private Route mockRoute;
  private Route mockRoute2;

  @Before
  public void setUp() {
    this.mockRoute = mock(Route.class);
    this.mockRoute2 = mock(Route.class);
  }

  @Test
  public void testNavigation() throws Exception {
    final Router router = new Router();
    router.register(RouteNames.INDEX, mockRoute);

    router.transitionTo(RouteNames.INDEX, "test", 1);

    verify(mockRoute, times(1)).transition(RouteNames.INDEX, "test", 1);
  }

  @Test
  public void testMultipleTargets() throws Exception {
    final Router router = new Router();
    router.register(RouteNames.INDEX, mockRoute);
    router.register(RouteNames.INDEX, mockRoute2);

    router.transitionTo(RouteNames.INDEX, "test", 1);

    verify(mockRoute, times(1)).transition(RouteNames.INDEX, "test", 1);
    verify(mockRoute2, times(1)).transition(RouteNames.INDEX, "test", 1);

  }

  @Test
  public void testRouting() {
    final Router router = new Router();
    router.register(RouteNames.ABOUT_WINDOW, mockRoute);

    router.transitionTo(RouteNames.INDEX, "test", 1);

    verify(mockRoute, times(0)).transition(any());
  }

}

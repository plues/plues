package de.hhu.stups.plues.routes;

import com.google.inject.Inject;

import de.hhu.stups.plues.ui.controller.MainController;

public class MainControllerRoute implements Route {

  private final MainController mainController;

  @Inject
  public MainControllerRoute(final MainController mainController) {
    this.mainController = mainController;
  }

  @Override
  public void transition(final RouteNames routeName, final Object... args) {
    mainController.activateController(routeName, args);
  }
}

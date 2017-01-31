package de.hhu.stups.plues.routes;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.ui.controller.Activatable;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import java.util.Optional;


public class ControllerRoute implements Route {

  private final Stage stage;
  private final String tabId;

  /**
   * Defines the {@link Route} to navigate to a controller managed by the {@link
   * de.hhu.stups.plues.ui.controller.MainController#tabPane}. Each tab is accessed via its css
   * selector defined in {@link de.hhu.stups.plues.ui.controller.MainController}.
   *
   * @param tabId The controller's tab id (css selector) within the tab pane.
   */
  @Inject
  public ControllerRoute(final Stage stage, @Assisted final String tabId) {
    this.stage = stage;
    this.tabId = tabId;
  }

  /**
   * @param routeName The {@link RouteNames route name}.
   * @param args First param is an array of strings, i.e. the courses. Second parameter is the
   *             {@link de.hhu.stups.plues.prob.ResultState}. Third parameter is an optional boolean
   *             value defining if the corresponding controller should be brought to front or e.g.
   *             some task should run in background. The default value is true.
   */
  @Override
  public void transition(final RouteNames routeName, final Object... args) {
    final TabPane tabPane = (TabPane) stage.getScene().lookup("#tabPane");
    final Optional<Tab> optionalTab = tabPane.getTabs().stream()
        .filter(tab -> tabId.equals(tab.getId())).findFirst();
    if (optionalTab.isPresent()) {
      selectTab(tabPane, optionalTab.get(), args);
      ((Activatable) optionalTab.get().getContent()).activateController(routeName, args);
    }
  }

  private void selectTab(final TabPane tabPane, final Tab tab, final Object... args) {
    final boolean bringToFront = args.length != 3 || (boolean) args[2];
    if (bringToFront) {
      tabPane.getSelectionModel().select(tab);
    }
  }
}

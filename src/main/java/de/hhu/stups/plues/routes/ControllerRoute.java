package de.hhu.stups.plues.routes;

import com.google.inject.Inject;

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
   * @param stage The application's stage.
   * @param tabId The controller's tab id within the tab pane.
   */
  @Inject
  public ControllerRoute(final Stage stage, final String tabId) {
    this.stage = stage;
    this.tabId = tabId;
  }

  @Override
  public void transition(final Object... args) {
    final TabPane tabPane = (TabPane) stage.getScene().lookup("#tabPane");
    final Optional<Tab> optionalTab = tabPane.getTabs().stream()
        .filter(tab -> tabId.equals(tab.getId())).findFirst();
    if (optionalTab.isPresent()) {
      tabPane.getSelectionModel().select(optionalTab.get());
      ((Activatable) optionalTab.get().getContent()).activateController(args);
    }
  }
}

package de.hhu.stups.plues.routes;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.controller.Activatable;

import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class ControllerRoute implements Route {

  private final Stage stage;
  private final int tabPaneIndex;

  /**
   * Defines the {@link Route} to navigate to a controller managed by the {@link
   * de.hhu.stups.plues.ui.controller.MainController#tabPane}. We use the indices within the tab
   * pane to address a specific tab. (We can't use css selectors in combination with a lookup for a
   * tab because {@link javafx.scene.control.Tab} does not inherit from {@link javafx.scene.Node}
   * which is the return type of the lookup)
   *
   * @param stage        The application's stage.
   * @param tabPaneIndex The controller's index in the tab pane's navigation.
   */
  @Inject
  public ControllerRoute(final Stage stage, final int tabPaneIndex) {
    this.stage = stage;
    this.tabPaneIndex = tabPaneIndex;
  }

  @Override
  public void transition(final Course... courses) {
    final TabPane tabPane = (TabPane) stage.getScene().lookup("#tabPane");
    tabPane.getSelectionModel().select(tabPaneIndex);
    ((Activatable) tabPane.getTabs().get(tabPaneIndex).getContent()).activateController(courses);
  }
}

package de.hhu.stups.plues.ui.components.conflictmatrix;

import de.hhu.stups.plues.Helpers;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.ResultState;
import de.hhu.stups.plues.routes.Router;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;

import java.util.ResourceBundle;

public class ResultGridCell extends Pane {

  private final ResourceBundle resources = ResourceBundle.getBundle("lang.conflictMatrix");
  private final ObjectProperty<ResultState> resultState;
  private ContextMenu contextMenu;
  private final Course[] courses;

  /**
   * A grid cell of the conflict matrix describing a specific result or an empty cell.
   */
  public ResultGridCell(final ResultState resultState, final Course... courses) {
    this.resultState = new SimpleObjectProperty<>(resultState);
    this.resultState.addListener((observable, oldValue, newValue) ->
        Platform.runLater(() -> updateResultGridCell(newValue, courses)));
    this.courses = courses;

    getStyleClass().add("matrix-cell");
    prefHeight(25.0);
    setMinWidth(40.0);
  }

  @SuppressWarnings("unused")
  private void showContextMenu(final MouseEvent event) {
    contextMenu.show(this, event.getScreenX(), event.getScreenY());
  }

  private void updateResultGridCell(final ResultState resultState, final Course... courses) {
    getChildren().clear();
    getStyleClass().setAll("matrix-cell");

    if (resultState == null) {
      return;
    }

    switch (resultState) {
      case SUCCEEDED:
      case FAILED:
      case TIMEOUT:
        setActiveGridCellPane(resultState, courses);
        break;
      case IMPOSSIBLE:
        setStaticImpossibleGridCell(courses[0].getName());
        break;
      case IMPOSSIBLE_COMBINATION:
        setImpossibleGridCell();
        break;
      default:
        break;
    }
  }

  /**
   * Create a grid pane cell for impossible combinations where major and minor courses are "equal",
   * i.e. the courses have the same short name.
   */
  private void setImpossibleGridCell() {
    getChildren().addAll(new Circle(5, 5, 2), new Circle(10, 5, 2), new Circle(5, 10, 2),
        new Circle(10, 10, 2), new Circle(5, 15, 2));
    getStyleClass().add("matrix-cell-ignored");
    final Label label = new Label();
    label.prefWidthProperty().bind(widthProperty());
    label.prefHeightProperty().bind(heightProperty());
    final Tooltip tooltip = new Tooltip(resources.getString("impossibleCombination"));
    Helpers.showTooltipOnEnter(label, tooltip, contextMenu.showingProperty());
    getChildren().add(label);
  }

  /**
   * Create a grid pane cell for statically known impossible combinations of courses.
   */
  private void setStaticImpossibleGridCell(final String courseName) {
    getChildren().addAll(new Circle(5, 5, 2), new Circle(10, 5, 2), new Circle(5, 10, 2),
        new Circle(10, 10, 2));
    getStyleClass().add("matrix-cell-impossible");
    final Label label = new Label();
    label.prefWidthProperty().bind(widthProperty());
    label.prefHeightProperty().bind(heightProperty());
    final Tooltip tooltip = new Tooltip(resources.getString("staticallyInfeasible1") + " "
        + courseName + " " + resources.getString("staticallyInfeasible2"));
    Helpers.showTooltipOnEnter(label, tooltip, contextMenu.showingProperty());
    getChildren().add(label);
  }

  /**
   * Create an active grid pane cell, i.e. either the feasibility is known or the computation ended
   * in a timeout. The pane's background color is set according to the result. Furthermore a tooltip
   * with major and minor course name is added.
   *
   * @param result      A {@link ResultState} object to distinguish between succeeded, failed and
   *                    timeouts.
   * @param courseNames An optional array of the major and minor course names.
   */
  private void setActiveGridCellPane(final ResultState result, final Course... courseNames) {
    final String styleClass;
    getChildren().add(new Circle(5, 5, 2));
    if (ResultState.FAILED.equals(result)) {
      getChildren().add(new Circle(10, 5, 2));
      styleClass = "matrix-cell-failure";
    } else if (ResultState.TIMEOUT.equals(result)) {
      getChildren().addAll(new Circle(10, 5, 2), new Circle(5, 10, 2));
      styleClass = "matrix-cell-timeout";
    } else {
      styleClass = "matrix-cell-success";
    }
    getStyleClass().add(styleClass);
    if (courseNames.length == 2) {
      final Label label = new Label();
      label.prefWidthProperty().bind(widthProperty());
      label.prefHeightProperty().bind(heightProperty());
      final Tooltip tooltip = new Tooltip(resources.getString("major") + " "
          + courseNames[0].getName() + "\n" + resources.getString("minor") + " "
          + courseNames[1].getName());
      Helpers.showTooltipOnEnter(label, tooltip, contextMenu.showingProperty());
      getChildren().add(label);
    }
  }

  public ResultState getResultState() {
    return resultState.get();
  }

  ContextMenu getContextMenu() {
    return contextMenu;
  }

  @SuppressWarnings("unused")
  public ObjectProperty<ResultState> resultStateProperty() {
    return resultState;
  }

  /**
   * Set the value of the {@link this#resultState} property.
   */
  public void setResultState(final ResultState resultState) {
    if (this.resultState.getValue() == null
        || this.resultState.getValue() != ResultState.IMPOSSIBLE_COMBINATION) {
      this.resultState.set(resultState);
    }
  }

  /**
   * Create the {@link ResultContextMenu context menu} with the given {@link Router} and set the
   * cell's mouse event.
   */
  public void setRouter(final Router router) {
    contextMenu = new ResultContextMenu(router, resultState, courses);
    setOnMouseClicked(event -> {
      if (event.getButton().equals(MouseButton.PRIMARY)) {
        showContextMenu(event);
      }
    });
  }
}

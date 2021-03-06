package de.hhu.stups.plues.ui.components.conflictmatrix;

import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.ResultState;
import de.hhu.stups.plues.ui.TooltipAllocator;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import org.fxmisc.easybind.EasyBind;

import java.util.ResourceBundle;

import javax.inject.Inject;

public class ResultGridCell extends Pane {

  private final ResourceBundle resources = ResourceBundle.getBundle("lang.conflictMatrix");
  private final ObjectProperty<ResultState> resultState
      = new SimpleObjectProperty<>(ResultState.UNKNOWN);

  private final BooleanProperty enabledProperty = new SimpleBooleanProperty(false);

  private final ResultContextMenu contextMenu;
  private Tooltip tooltip;

  /**
   * A grid cell of the conflict matrix describing a specific result or an empty cell.
   */
  @Inject
  public ResultGridCell(final ResultContextMenuFactory resultContextMenuFactory,
      @Assisted final Course... courses) {

    EasyBind.subscribe(this.resultState,
        newValue -> Platform.runLater(() -> updateResultGridCell(newValue, courses)));

    contextMenu = resultContextMenuFactory.create(courses);
    initializeContextMenu();

    getStyleClass().add("matrix-cell");
    prefHeight(25.0);
    setMinWidth(40.0);
  }

  private void initializeContextMenu() {
    final BooleanProperty contextMenuFocusedProperty = new SimpleBooleanProperty(false);

    contextMenu.resultStateProperty().bind(this.resultState);

    final ObjectProperty<MouseEvent> showContextMenuProperty = new SimpleObjectProperty<>();
    showContextMenuProperty.addListener((observable, oldValue, newValue) ->
        showContextMenu(newValue));

    ContextMenuListeners.setContextMenuListeners(this, contextMenu,
        contextMenuFocusedProperty, showContextMenuProperty);
  }

  @SuppressWarnings("unused")
  private void showContextMenu(final MouseEvent event) {
    if (!enabledProperty.get()) {
      return;
    }
    if (tooltip != null) {
      tooltip.hide();
    }
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
        setStaticImpossibleGridCell();
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
    tooltip = new Tooltip(resources.getString("legendImpossible"));
    TooltipAllocator.showTooltipOnEnter(label, tooltip, contextMenu.showingProperty());
    getChildren().add(label);
  }

  /**
   * Create a grid pane cell for statically known impossible combinations of courses.
   */
  private void setStaticImpossibleGridCell() {
    getChildren().addAll(new Circle(5, 5, 2), new Circle(10, 5, 2), new Circle(5, 10, 2),
        new Circle(10, 10, 2));
    getStyleClass().add("matrix-cell-impossible");
    final Label label = new Label();
    label.prefWidthProperty().bind(widthProperty());
    label.prefHeightProperty().bind(heightProperty());
    tooltip = new Tooltip(resources.getString("legendInfeasible"));
    TooltipAllocator.showTooltipOnEnter(label, tooltip, contextMenu.showingProperty());
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
      tooltip = new Tooltip(resources.getString("major") + ": "
          + courseNames[0].getName() + "\n" + resources.getString("minor") + ": "
          + courseNames[1].getName());
      TooltipAllocator.showTooltipOnEnter(label, tooltip, contextMenu.showingProperty());
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

  public BooleanProperty enabledProperty() {
    return enabledProperty;
  }

  public void setEnabled(final boolean enabled) {
    this.enabledProperty.set(enabled);
  }
}

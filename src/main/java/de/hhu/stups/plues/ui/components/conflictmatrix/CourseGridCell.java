package de.hhu.stups.plues.ui.components.conflictmatrix;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.TooltipAllocator;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import org.reactfx.EventSource;

public class CourseGridCell extends Pane {

  private static final String VERTICAL = "vertical";

  private final Course course;
  private final String orientation;
  private final CourseContextMenu courseContextMenu;
  private final Tooltip tooltip;

  public BooleanProperty enabledProperty() {
    return enabledProperty;
  }

  private final BooleanProperty enabledProperty = new SimpleBooleanProperty(false);

  /**
   * Create a grid cell representing a course name.
   *
   * @param orientation The orientation of the label, default is horizontal.
   */
  public CourseGridCell(final Course course,
                        final String orientation,
                        final EventSource<Course> checkCourseCombinations) {
    this.course = course;
    this.orientation = orientation;
    tooltip = new Tooltip(course.getFullName());

    final ObjectProperty<MouseEvent> showContextMenuProperty = new SimpleObjectProperty<>();
    showContextMenuProperty.addListener((observable, oldValue, newValue) ->
        showContextMenu(newValue));

    Platform.runLater(this::initializeGridCell);
    getStyleClass().add("matrix-cell");

    courseContextMenu = new CourseContextMenu(course, checkCourseCombinations);
    ContextMenuListeners.setContextMenuListeners(this, courseContextMenu,
        new SimpleBooleanProperty(false), showContextMenuProperty);
  }

  @SuppressWarnings("unused")
  private void showContextMenu(final MouseEvent event) {
    if (!enabledProperty.get() && course.isCombinable()) {
      return;
    }
    if (tooltip != null) {
      tooltip.hide();
    }
    courseContextMenu.show(this, event.getScreenX(), event.getScreenY());
  }

  @SuppressWarnings("unused")
  private void initializeGridCell() {
    final Label label = new Label("  " + course.getKey() + "  ");
    if (VERTICAL.equals(orientation)) {
      label.setRotate(270.0);
      label.setTranslateY(100.0);
      label.setTranslateX(-70.0);
      label.setPrefWidth(200.0);
    } else {
      setPrefHeight(25.0);
    }
    final Group group = new Group(label);
    TooltipAllocator.showTooltipOnEnter(group, tooltip, new SimpleBooleanProperty(false));
    getChildren().add(group);
  }
}

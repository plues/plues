package de.hhu.stups.plues.ui.components.conflictmatrix;

import de.hhu.stups.plues.Helpers;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;

public class CourseGridCell extends Pane {

  private static final String VERTICAL = "vertical";

  private final String courseKey;
  private final String courseName;
  private final String orientation;

  /**
   * Create a grid cell representing a course name.
   *
   * @param orientation The orientation of the label, default is horizontal.
   */
  public CourseGridCell(final String courseKey, final String courseName, final String orientation) {
    this.courseKey = courseKey;
    this.courseName = courseName;
    this.orientation = orientation;
    Platform.runLater(this::initializeGridCell);
    getStyleClass().add("matrix-cell");
  }

  @SuppressWarnings("unused")
  private void initializeGridCell() {

    final Label label = new Label("  " + courseKey + "  ");
    if (VERTICAL.equals(orientation)) {
      label.setRotate(270.0);
      label.setTranslateY(100.0);
      label.setTranslateX(-70.0);
      label.setPrefWidth(200.0);
    } else {
      setPrefHeight(25.0);
    }
    final Group group = new Group(label);
    Helpers.showTooltipOnEnter(group, new Tooltip(courseName), new SimpleBooleanProperty(false));
    getChildren().add(group);
  }

}

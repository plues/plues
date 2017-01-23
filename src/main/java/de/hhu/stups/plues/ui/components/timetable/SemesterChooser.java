package de.hhu.stups.plues.ui.components.timetable;

import javafx.collections.ObservableList;
import javafx.scene.control.ToggleButton;
import org.controlsfx.control.SegmentedButton;

/**
 * A component to choose semesters.
 */
public class SemesterChooser extends SegmentedButton {
  public SemesterChooser() {
    super();
  }

  public SemesterChooser(ToggleButton... semesters) {
    super(semesters);
  }

  public SemesterChooser(ObservableList<ToggleButton> semesters) {
    super(semesters);
  }
}

package de.hhu.stups.plues.ui.components.reports;

import javafx.collections.ListChangeListener;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;

class PersistentToggleGroup extends ToggleGroup {
  PersistentToggleGroup() {
    super();
    getToggles().addListener((ListChangeListener<Toggle>) change -> {
      while (change.next()) {
        for (final Toggle addedToggle : change.getAddedSubList()) {
          ((ToggleButton) addedToggle).addEventFilter(MouseEvent.MOUSE_RELEASED, mouseEvent -> {
            if (addedToggle.equals(getSelectedToggle())) {
              mouseEvent.consume();
            }
          });
        }
      }
    });
  }
}

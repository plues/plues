package de.hhu.stups.plues.ui.components.timetable;

import javafx.beans.Observable;
import javafx.beans.binding.ListBinding;
import javafx.beans.binding.SetBinding;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.event.EventHandler;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import org.controlsfx.control.SegmentedButton;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A component to choose semesters.
 */
public class SemesterChooser extends Region {

  private final SetProperty<Integer> selectedSemesters = new SimpleSetProperty<>();
  private final SetProperty<Integer> conflictedSemesters
      = new SimpleSetProperty<>(FXCollections.observableSet());
  private final SegmentedButton segmentedButton;

  private SelectedSemestersBinding selectedSemestersBinding;

  /**
   * Create a new semester choosing component.
   * By default values from 1 to 6 are shown.
   */
  public SemesterChooser() {
    super();
    ObservableList<ToggleButton> buttons = buildButtons();
    segmentedButton = new SegmentedButton(buttons);
    init();
  }

  private ObservableList<ToggleButton> buildButtons() {
    return IntStream.rangeClosed(1, 6).mapToObj(value -> {
      final String stringVaue = String.valueOf(value);
      final ToggleButton toggleButton = new ToggleButton(stringVaue);
      toggleButton.setUserData(stringVaue);
      toggleButton.setSelected(value == 1);
      return toggleButton;
    }).collect(
        Collectors.collectingAndThen(Collectors.toList(),
            FXCollections::observableArrayList));
  }

  private void init() {
    addSegmentedButton();

    segmentedButton.setToggleGroup(null);
    selectedSemestersBinding = new SelectedSemestersBinding();
    selectedSemesters.bind(selectedSemestersBinding);

    final EventHandler<MouseEvent> handleMouseClicked = this::handleMouseClicked;
    final EventHandler<KeyEvent> handleKeyPressed = this::handleKeyPressed;

    segmentedButton.getButtons().forEach(o -> {
      o.addEventFilter(MouseEvent.MOUSE_CLICKED, handleMouseClicked);
      o.addEventFilter(KeyEvent.KEY_PRESSED, handleKeyPressed);
    });

    conflictedSemesters.addListener(this::handleConflictedSemesters);
  }

  private void addSegmentedButton() {
    this.getChildren().add(segmentedButton);
    this.setHeight(segmentedButton.getHeight());
    this.setWidth(segmentedButton.getWidth());
  }

  @SuppressWarnings("unused")
  private void handleConflictedSemesters(final ObservableValue<?> observable,
                                         final ObservableSet<Integer> oldValue,
                                         final ObservableSet<Integer> newValue) {
    segmentedButton.getButtons().forEach(toggle -> {
      final int value = Integer.parseInt((String) toggle.getUserData());

      if (newValue.contains(value)) {
        toggle.getStyleClass().add("conflicted-semester");
      } else {
        toggle.getStyleClass().remove("conflicted-semester");
      }
    });
  }

  @SuppressWarnings("unused")
  private void handleMouseClicked(final MouseEvent event) {
    final ToggleButton button = (ToggleButton) event.getSource();

    final boolean newState;
    if (!event.isControlDown()) {
      newState = button.isSelected();
      deselectAll();
    } else {
      newState = !button.isSelected();
    }

    button.setSelected(newState);
    event.consume();
  }

  private void deselectAll() {
    selectedSemesters.unbind();
    segmentedButton.getButtons().forEach(toggleButton -> toggleButton.setSelected(false));
    selectedSemesters.bind(selectedSemestersBinding);
  }

  @SuppressWarnings("unused")
  private void handleKeyPressed(final KeyEvent event) {
    if (event.getCode() != KeyCode.SPACE) {
      return;
    }
    final ToggleButton button = (ToggleButton) event.getSource();
    final boolean newState = !button.isSelected();

    if (!event.isControlDown()) {
      deselectAll();
    }

    button.setSelected(newState);
    event.consume();
  }

  public ObservableSet<Integer> getSelectedSemesters() {
    return selectedSemesters.get();
  }

  public SetProperty<Integer> selectedSemestersProperty() {
    return selectedSemesters;
  }

  /**
   * Set the semesters semesters that should be selected in the component.
   * @param selection Set of semesters to be selected.
   */
  public void setSelectedSemesters(final ObservableSet<Integer> selection) {
    selectedSemesters.unbind();
    segmentedButton.getButtons().forEach(toggleButton
        -> toggleButton.setSelected(
            selection.contains(Integer.parseInt((String) toggleButton.getUserData()))));
    selectedSemesters.bind(selectedSemestersBinding);
  }

  /**
   * Set the semestesr that should be highlighted as containing a conflict.
   * @param semesters Set of semesters that should be highlighted.
   */
  public void setConflictedSemesters(final ObservableSet<Integer> semesters) {
    this.conflictedSemesters.set(semesters);
  }

  public SetProperty<Integer> conflictedSemestersProperty() {
    return this.conflictedSemesters;
  }

  ObservableList<ToggleButton> getButtons() {
    return FXCollections.unmodifiableObservableList(segmentedButton.getButtons());
  }

  private class SelectedSemestersBinding extends SetBinding<Integer> {

    private final ListBinding<ToggleButton> binding;

    SelectedSemestersBinding() {
      // if this reference is removed the binding will get garbage collected and thus won't work
      this.binding = new ButtonBinding();
      bind(binding);
    }

    @Override
    protected ObservableSet<Integer> computeValue() {
      return FXCollections.observableSet(this.binding.stream()
          .filter(ToggleButton::isSelected)
          .mapToInt(button -> Integer.parseInt((String) button.getUserData())).boxed()
          .collect(Collectors.toSet()));
    }

    private class ButtonBinding extends ListBinding<ToggleButton> {
      ButtonBinding() {
        bind(segmentedButton.getButtons());
      }

      @Override
      protected ObservableList<ToggleButton> computeValue() {
        return FXCollections.observableList(segmentedButton.getButtons(),
          button -> new Observable[] { button.selectedProperty() });
      }
    }
  }
}

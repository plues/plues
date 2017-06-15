package de.hhu.stups.plues.ui.components.timetable;

import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ListBinding;
import javafx.beans.binding.SetBinding;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleSetProperty;
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
import org.fxmisc.easybind.EasyBind;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A component to choose semesters.
 */
public class SemesterChooser extends Region {

  private final SetProperty<Integer> selectedSemesters
      = new SimpleSetProperty<>(FXCollections.observableSet());
  private final SetProperty<Integer> conflictedSemesters
      = new SimpleSetProperty<>(FXCollections.observableSet());
  private final SegmentedButton segmentedButton;

  private SelectedSemestersBinding selectedSemestersBinding;

  // List of bindings used to set the styleClass of each toggle button
  // This field is needed to avoid the bindings getting garbage collected due to weak references
  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private final List<BooleanBinding> bindings = new ArrayList<>();

  /**
   * Create a new semester choosing component.
   * By default values from 1 to 6 are shown.
   */
  @SuppressWarnings("WeakerAccess")
  public SemesterChooser() {
    super();
    segmentedButton = new SegmentedButton();
  }

  private ObservableList<ToggleButton> buildButtons(final List<Integer> semesters) {
    return semesters.stream().map(value -> {
      final String stringVaue = String.valueOf(value);
      final ToggleButton toggleButton = new ToggleButton(stringVaue);
      //
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

    segmentedButton.getButtons().forEach(button -> {
      button.addEventFilter(MouseEvent.MOUSE_CLICKED, handleMouseClicked);
      button.addEventFilter(KeyEvent.KEY_PRESSED, handleKeyPressed);
      button.setFocusTraversable(false);
    });

    this.setupSubscription(conflictedSemesters);
  }

  private void setupSubscription(final ObservableSet<Integer> semesters) {
    segmentedButton.getButtons().forEach(o -> {
      //
      final BooleanBinding conflictProperty = Bindings.createBooleanBinding(() ->
          semesters.contains(Integer.parseInt((String) o.getUserData())), semesters);
      //
      // collect bindings in an instance variable to avoid collection due to weak references
      this.bindings.add(conflictProperty);
      //
      EasyBind.includeWhen(o.getStyleClass(), "conflicted-semester", conflictProperty);
    });
  }

  private void addSegmentedButton() {
    this.getChildren().add(segmentedButton);
    this.setHeight(segmentedButton.getHeight());
    this.setWidth(segmentedButton.getWidth());
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
   *
   * @param selection Set of semesters to be selected.
   */
  public void setSelectedSemesters(final ObservableSet<Integer> selection) {
    selectedSemesters.unbind();
    segmentedButton.getButtons().forEach(toggleButton ->
        toggleButton.setSelected(
            selection.contains(Integer.parseInt((String) toggleButton.getUserData()))));
    selectedSemesters.bind(selectedSemestersBinding);
  }

  /**
   * Set the semesters that should be highlighted as containing a conflict.
   *
   * @param semesters Set of semesters that should be highlighted.
   */
  void setConflictedSemesters(final ObservableSet<Integer> semesters) {
    this.conflictedSemesters.set(semesters);
  }

  public SetProperty<Integer> conflictedSemestersProperty() {
    return this.conflictedSemesters;
  }

  ObservableList<ToggleButton> getButtons() {
    return FXCollections.unmodifiableObservableList(segmentedButton.getButtons());
  }

  /**
   * Set the list of semesters to be displayed by the component.
   *
   * @param semesters List of integers
   */
  public void setSemesters(final List<Integer> semesters) {
    final ObservableList<ToggleButton> buttons = buildButtons(semesters);
    segmentedButton.getButtons().setAll(buttons);
    init();
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
            button -> new Observable[] {button.selectedProperty()});
      }
    }
  }
}

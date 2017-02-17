package de.hhu.stups.plues.ui.components.timetable;

import javafx.beans.Observable;
import javafx.beans.binding.ListBinding;
import javafx.beans.binding.SetBinding;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.event.EventHandler;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import org.controlsfx.control.SegmentedButton;

import java.util.stream.Collectors;

/**
 * A component to choose semesters.
 */
public class SemesterChooser extends SegmentedButton {

  private final SetProperty<Integer> selectedSemesters = new SimpleSetProperty<>();
  private SelectedSemestersBinding selectedSemestersBinding;

  @SuppressWarnings("unused")
  public SemesterChooser() {
    super();
    init();
  }

  @SuppressWarnings("unused")
  public SemesterChooser(final ToggleButton... semesters) {
    super(semesters);
    init();
  }

  @SuppressWarnings({"unused","WeakerAccess"})
  public SemesterChooser(final ObservableList<ToggleButton> semesters) {
    super(semesters);
    init();
  }

  private void init() {
    setToggleGroup(null);
    selectedSemestersBinding = new SelectedSemestersBinding();
    selectedSemesters.bind(selectedSemestersBinding);

    final EventHandler<MouseEvent> handleMouseClicked = this::handleMouseClicked;
    final EventHandler<KeyEvent> handleKeyPressed = this::handleKeyPressed;

    getButtons().addListener((ListChangeListener<ToggleButton>) c -> {
      while (c.next()) {
        c.getAddedSubList().forEach(o -> {
          o.addEventFilter(MouseEvent.MOUSE_CLICKED, handleMouseClicked);
          o.addEventFilter(KeyEvent.KEY_PRESSED, handleKeyPressed);
        });
      }
      c.getRemoved().forEach(o -> {
        o.removeEventFilter(MouseEvent.MOUSE_CLICKED, handleMouseClicked);
        o.removeEventFilter(KeyEvent.KEY_PRESSED, handleKeyPressed);
      });
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
    getButtons().forEach(toggleButton -> toggleButton.setSelected(false));
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
    getButtons().forEach(toggleButton
        -> toggleButton.setSelected(
            selection.contains(Integer.parseInt((String) toggleButton.getUserData()))));
    selectedSemesters.bind(selectedSemestersBinding);
  }

  /**
   * Set the semestesr that should be highlighted as containing a conflict.
   * @param semesters Set of semesters that should be highlighted.
   */
  public void setConflictedSemesters(final ObservableSet<Integer> semesters) {
    getButtons().forEach(toggle -> {
      final int value = Integer.valueOf((String) toggle.getUserData());

      if (semesters.contains(value)) {
        toggle.getStyleClass().add("conflicted-semester");
      } else {
        toggle.getStyleClass().remove("conflicted-semester");
      }
    });
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
          .mapToInt(button -> Integer.valueOf((String) button.getUserData())).boxed()
          .collect(Collectors.toSet()));
    }

    private class ButtonBinding extends ListBinding<ToggleButton> {
      ButtonBinding() {
        bind(getButtons());
      }

      @Override
      protected ObservableList<ToggleButton> computeValue() {
        return FXCollections.observableList(getButtons(),
          button -> new Observable[] { button.selectedProperty() });
      }
    }
  }
}

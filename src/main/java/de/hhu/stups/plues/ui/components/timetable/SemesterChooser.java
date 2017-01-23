package de.hhu.stups.plues.ui.components.timetable;

import javafx.beans.Observable;
import javafx.beans.binding.ListBinding;
import javafx.beans.binding.SetBinding;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.scene.control.ToggleButton;
import org.controlsfx.control.SegmentedButton;

import java.util.stream.Collectors;

/**
 * A component to choose semesters.
 */
public class SemesterChooser extends SegmentedButton {

  private final SetProperty<Integer> selectedSemesters = new SimpleSetProperty<>();

  @SuppressWarnings("unused")
  public SemesterChooser() {
    super();
    init();
  }

  @SuppressWarnings("unused")
  public SemesterChooser(ToggleButton... semesters) {
    super(semesters);
    init();
  }

  @SuppressWarnings("unused")
  public SemesterChooser(ObservableList<ToggleButton> semesters) {
    super(semesters);
    init();
  }

  private void init() {
    setToggleGroup(null);
    selectedSemesters.bind(new SelectedSemestersBinding());
  }

  public ObservableSet<Integer> getSelectedSemesters() {
    return selectedSemesters.get();
  }

  public SetProperty<Integer> selectedSemestersProperty() {
    return selectedSemesters;
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

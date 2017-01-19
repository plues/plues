package de.hhu.stups.plues.ui.components;

import static java.util.stream.Collectors.toList;
import static javafx.collections.FXCollections.emptyObservableList;
import static javafx.collections.FXCollections.observableArrayList;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.layout.Inflater;
import javafx.beans.Observable;
import javafx.beans.binding.ListBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public class AbstractUnitFilter extends VBox implements Initializable {

  private final ToggleGroup filterGroup;
  private final ListProperty<AbstractUnit> selectedAbstractUnits;
  private final ListProperty<AbstractUnit> abstractUnits;
  private final SimpleListProperty<SelectableAbstractUnit> selectableAbstractUnits;
  private final SimpleListProperty<Course> courseFilter;

  @FXML
  @SuppressWarnings("unused")
  private TextField query;
  @FXML
  @SuppressWarnings("unused")
  private RadioButton selected;
  @FXML
  @SuppressWarnings("unused")
  private RadioButton notSelected;
  @FXML
  @SuppressWarnings("unused")
  private RadioButton all;
  @FXML
  @SuppressWarnings("unused")
  private CheckBox selectedCoursesOnly;
  @FXML
  @SuppressWarnings("unused")
  private TableView<SelectableAbstractUnit> units;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<SelectableAbstractUnit, Boolean> checkboxColumn;

  /**
   * AbstractUnitFilter component.
   * Show a list of abstract units and allow the user to select one or more of them.
   *
   * @param inflater Inflater
   */
  @Inject
  public AbstractUnitFilter(final Inflater inflater) {
    abstractUnits = new SimpleListProperty<>(observableArrayList());
    selectedAbstractUnits = new SimpleListProperty<>();
    filterGroup = new ToggleGroup();
    selectableAbstractUnits = new SimpleListProperty<>(emptyObservableList());
    courseFilter = new SimpleListProperty<>(emptyObservableList());

    inflater.inflate("components/AbstractUnitFilter", this, this, "filter", "Column");
  }

  private ObservableList<AbstractUnit> getAbstractUnits() {
    return abstractUnits.get();
  }

  /**
   * Setter for abstract units. Required to display content.
   *
   * @param abstractUnits List of abstract units to be displayed in TableView
   */
  public void setAbstractUnits(final List<AbstractUnit> abstractUnits) {
    this.abstractUnits.setAll(abstractUnits);
  }

  public ListProperty<AbstractUnit> abstractUnitsProperty() {
    return abstractUnits;
  }

  public ObservableList<AbstractUnit> getSelectedAbstractUnits() {
    return selectedAbstractUnits.get();
  }

  public ReadOnlyListProperty<AbstractUnit> selectedAbstractUnitsProperty() {
    return selectedAbstractUnits;
  }

  public ObservableList<Course> getCourseFilter() {
    return courseFilter.get();
  }

  public SimpleListProperty<Course> courseFilterProperty() {
    return courseFilter;
  }

  /**
   * Setter for courseFilter.
   * @param courseFilter List of courses to be filtered by in TableView
   */
  public void setCourseFilter(ObservableList<Course> courseFilter) {
    this.courseFilter.set(courseFilter);
  }

  /**
   * OnClick method to remove selection and return to all units view.
   */
  @FXML
  @SuppressWarnings("unused")
  public void resetSelection() {
    selectableAbstractUnits.forEach(selectableAbstractUnit
        -> selectableAbstractUnit.setSelected(false));
    selectedAbstractUnits.clear();
    query.clear();

    selected.setSelected(false);
    notSelected.setSelected(false);
    all.setSelected(true);
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    selected.setToggleGroup(filterGroup);
    notSelected.setToggleGroup(filterGroup);
    all.setToggleGroup(filterGroup);

    checkboxColumn.setCellFactory(CheckBoxTableCell.forTableColumn(checkboxColumn));

    selectableAbstractUnits.bind(new ListBinding<SelectableAbstractUnit>() {
      // extractor used to compute an observable list that propagates changes on the extracted
      // property to the observers of the list
      final Callback<SelectableAbstractUnit, Observable[]> extractor
          = (SelectableAbstractUnit param) -> new Observable[] {param.selectedProperty()};

      {
        bind(abstractUnits);
      }

      @Override
      public void dispose() {
        super.dispose();
        unbind(abstractUnits);
      }


      // NOTE: A change to the abstractUnits list, this binding is bound to, will recreate all
      // SelectableAbstractUnit objects. This behaviour will loose the state of all
      // selectedProperties.
      @Override
      protected ObservableList<SelectableAbstractUnit> computeValue() {
        return FXCollections.observableList(
          abstractUnits.stream().map(SelectableAbstractUnit::new)
            .collect(toList()), extractor);
      }
    });


    final FilteredList<SelectableAbstractUnit> filteredUnits
        = new FilteredList<>(selectableAbstractUnits);
    filteredUnits.predicateProperty().bind(
        new ObjectBinding<Predicate<? super SelectableAbstractUnit>>() {
        {
          bind(query.textProperty(), all.selectedProperty(), selected.selectedProperty(),
              notSelected.selectedProperty());
          bind(selectedCoursesOnly.selectedProperty(), courseFilter);
        }

        @Override
        public void dispose() {
          super.dispose();
          unbind(query.textProperty(), all.selectedProperty(), selected.selectedProperty(),
              notSelected.selectedProperty());
          unbind(selectedCoursesOnly.selectedProperty(), courseFilter);
        }

        @Override
        protected Predicate<? super SelectableAbstractUnit> computeValue() {
          return selectableAbstractUnit -> selectableAbstractUnit.matches(query, all.isSelected(),
                selected.isSelected(), notSelected.isSelected(),
                selectedCoursesOnly.isSelected(), getCourseFilter());
        }
      });

    units.itemsProperty().bind(new SimpleListProperty<>(filteredUnits));

    selectedAbstractUnits.bind(new ListBinding<AbstractUnit>() {
      {
        bind(selectableAbstractUnits);
      }

      @Override
      protected ObservableList<AbstractUnit> computeValue() {
        return
          selectableAbstractUnits.filtered(SelectableAbstractUnit::isSelected).stream()
            .map(SelectableAbstractUnit::getAbstractUnit)
            .collect(
              Collectors.collectingAndThen(
                Collectors.toList(), FXCollections::observableList));
      }
    });
  }

  @SuppressWarnings("WeakerAccess")
  public static final class SelectableAbstractUnit {
    private final BooleanProperty selected;
    private final AbstractUnit abstractUnit;
    private final Set<Course> abstractUnitCourses;

    SelectableAbstractUnit(final AbstractUnit abstractUnit) {
      this.selected = new SimpleBooleanProperty(false);
      this.abstractUnit = abstractUnit;
      abstractUnitCourses = abstractUnit.getModules().stream()
        .flatMap(module -> module.getCourses().stream())
        .collect(Collectors.toSet());
    }

    private boolean isSelected() {
      return selected.get();
    }

    public void setSelected(final boolean selected) {
      this.selected.set(selected);
    }

    public BooleanProperty selectedProperty() {
      return selected;
    }

    public String getTitle() {
      return this.abstractUnit.getTitle();
    }

    public String getKey() {
      return this.abstractUnit.getKey();
    }

    boolean matches(final TextField query, final boolean all, final boolean showSelected,
                    final boolean showNotSelected, final boolean selectedCoursesOnly,
                    final List<Course> courseFilter) {
      return this.titleOrKeyMatchesQuery(query)
        && this.checkboxMatchesCriteria(all, showSelected, showNotSelected)
        && this.selectedCoursesCriteria(courseFilter, selectedCoursesOnly);
    }

    private boolean checkboxMatchesCriteria(final boolean all, final boolean showSelected,
                                            final boolean showNotSelected) {
      final boolean checked = this.isSelected();
      final boolean showIfSelected = checked && showSelected;
      final boolean showIfNotSelected = !checked && showNotSelected;
      return all || showIfSelected || showIfNotSelected;
    }

    private boolean titleOrKeyMatchesQuery(final TextField query) {
      final String lowerCaseTitle = this.abstractUnit.getTitle().toLowerCase();
      final String lowerCaseKey = this.abstractUnit.getKey().toLowerCase();
      final String text = query.getText().toLowerCase();
      return text.isEmpty() || lowerCaseTitle.contains(text) || lowerCaseKey.contains(text);
    }

    private boolean selectedCoursesCriteria(final List<Course> selectedCourses,
                                            final boolean selectedCoursesOnly) {
      return !selectedCoursesOnly
        || selectedCourses.stream().anyMatch(abstractUnitCourses::contains);

    }

    private AbstractUnit getAbstractUnit() {
      return abstractUnit;
    }
  }
}

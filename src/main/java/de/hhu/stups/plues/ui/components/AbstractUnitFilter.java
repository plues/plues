package de.hhu.stups.plues.ui.components;

import static java.util.stream.Collectors.toList;
import static javafx.collections.FXCollections.emptyObservableList;
import static javafx.collections.FXCollections.observableArrayList;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.ui.layout.Inflater;
import javafx.beans.Observable;
import javafx.beans.binding.ListBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

@SuppressWarnings("WeakerAccess")
public class AbstractUnitFilter extends VBox implements Initializable {

  private final ToggleGroup filterGroup;
  private final ListProperty<AbstractUnit> selectedAbstractUnits;
  private final ListProperty<AbstractUnit> abstractUnits;
  private final SimpleListProperty<SelectableAbstractUnit> selectableAbstractUnits;
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
  private TableView<SelectableAbstractUnit> units;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<SelectableAbstractUnit, Boolean> checkboxColumn;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<SelectableAbstractUnit, String> abstractUnitTitleColumn;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<SelectableAbstractUnit, String> abstractUnitKeyColumn;

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

    inflater.inflate("components/AbstractUnitFilter", this, this, "filter");
  }

  private ObservableList<AbstractUnit> getAbstractUnits() {
    return abstractUnits.get();
  }

  /**
   * Setter for abstract units. Required to display content.
   *
   * @param abstractUnits List of abstract units to be displayed in TableView
   */
  void setAbstractUnits(final List<AbstractUnit> abstractUnits) {
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
    checkboxColumn.setCellValueFactory(new PropertyValueFactory<>("selected"));

    abstractUnitTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
    abstractUnitKeyColumn.setCellValueFactory(new PropertyValueFactory<>("key"));

    selectableAbstractUnits.bind(new ListBinding<SelectableAbstractUnit>() {
      {
        bind(abstractUnits);
      }

      @Override
      public void dispose() {
        super.dispose();
        unbind(abstractUnits);
      }


      // extractor used to compute an observable list that propagates changes on the extracted
      // property to the observers of the list
      final Callback<SelectableAbstractUnit, Observable[]> extractor
          = (SelectableAbstractUnit param) -> new Observable[] {param.selectedProperty()};

      @Override
      protected ObservableList<SelectableAbstractUnit> computeValue() {
        return FXCollections.observableList(
          abstractUnits.parallelStream().map(SelectableAbstractUnit::new)
            .collect(toList()), extractor);
      }
    });


    final ListBinding<SelectableAbstractUnit> tableViewBinding
        = new ListBinding<SelectableAbstractUnit>() {
          {
            bind(query.textProperty(), all.selectedProperty(), selected.selectedProperty(),
                notSelected.selectedProperty(), selectableAbstractUnits);
          }

          @Override
          protected ObservableList<SelectableAbstractUnit> computeValue() {
            return selectableAbstractUnits.get().filtered(selectableAbstractUnit
                -> selectableAbstractUnit.matches(query, all.isSelected(),
              selected.isSelected(), notSelected.isSelected()));
          }
        };
    units.itemsProperty().bind(tableViewBinding);

    selectedAbstractUnits.bind(new ListBinding<AbstractUnit>() {
      {
        bind(selectableAbstractUnits);
      }

      @Override
      protected ObservableList<AbstractUnit> computeValue() {
        return
          selectableAbstractUnits.filtered(SelectableAbstractUnit::isSelected).parallelStream()
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

    SelectableAbstractUnit(final AbstractUnit abstractUnit) {
      this.selected = new SimpleBooleanProperty(false);
      this.abstractUnit = abstractUnit;
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
                    final boolean showNotSelected) {
      return this.titleMatchesQuery(query)
        && this.checkboxMatchesCriteria(all, showSelected, showNotSelected);
    }

    private boolean checkboxMatchesCriteria(final boolean all, final boolean showSelected,
                                            final boolean showNotSelected) {
      final boolean checked = this.isSelected();
      final boolean showIfSelected = checked && showSelected;
      final boolean showIfNotSelected = !checked && showNotSelected;
      return all || showIfSelected || showIfNotSelected;
    }

    private boolean titleMatchesQuery(final TextField query) {
      final String lowerCaseTitle = this.abstractUnit.getTitle().toLowerCase();
      final String lowerCaseKey = this.abstractUnit.getKey().toLowerCase();
      final String text = query.getText().toLowerCase();
      return text.isEmpty() || lowerCaseTitle.contains(text) || lowerCaseKey.contains(text);
    }

    private AbstractUnit getAbstractUnit() {
      return abstractUnit;
    }
  }
}

package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.ListBinding;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AbstractUnitFilter extends VBox implements Initializable {

  private final ToggleGroup filterGroup = new ToggleGroup();
  private ObservableList<RowEntry> allItems = FXCollections.observableArrayList();
  private ObservableList<RowEntry> displayedItems = FXCollections.observableArrayList();
  private ObservableList<RowEntry> selectedItems = FXCollections.observableArrayList();
  private SimpleListProperty<RowEntry> listProperty;

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
  private TableView<RowEntry> units;

  @Inject
  public AbstractUnitFilter(final Inflater inflater) {
    inflater.inflate("components/AbstractUnitFilter", this, this, "main");
  }

  /**
   * OnClick method to select all units again.
   */
  @FXML
  public void allItems() {
    displayedItems.clear();
    displayedItems.addAll(allItems);
  }

  /**
   * OnClick method to filter only selected units.
   */
  @FXML
  public void filterBySelected() {
    displayedItems.clear();
    displayedItems.addAll(selectedItems);
  }

  /**
   * OnClick method to filter only not-selected units.
   */
  @FXML
  public void filterByUnselected() {
    displayedItems.clear();
    displayedItems.addAll(allItems.filtered(rowEntry -> !selectedItems.contains(rowEntry)));
  }

  /**
   * OnClick method to remove selection and return to all units view.
   */
  @FXML
  public void resetSelection() {
    allItems.forEach(rowEntry -> ((CheckBox) rowEntry.getCheckbox()).setSelected(false));
    selectedItems.clear();
    query.clear();

    selected.setSelected(false);
    notSelected.setSelected(false);
    all.setSelected(true);
    allItems();
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    selected.setToggleGroup(filterGroup);
    notSelected.setToggleGroup(filterGroup);
    all.setToggleGroup(filterGroup);

    units.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    listProperty = new SimpleListProperty<>(displayedItems);

    final TableColumn<RowEntry, CheckBox> checkBoxTableColumn = new TableColumn<>("Check");
    final TableColumn<RowEntry, String> nameTableColumn = new TableColumn<>("Abstract Unit Title");

    checkBoxTableColumn.setCellValueFactory(new PropertyValueFactory<>("checkbox"));
    checkBoxTableColumn.setSortable(false);
    checkBoxTableColumn.setResizable(false);
    checkBoxTableColumn.setPrefWidth(50);
    checkBoxTableColumn.setStyle("-fx-alignment: CENTER");
    nameTableColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
    nameTableColumn.setSortable(false);
    nameTableColumn.setPrefWidth(300);
    nameTableColumn.setResizable(false);

    units.getColumns().addAll(checkBoxTableColumn, nameTableColumn);

    ListBinding<RowEntry> binding = new ListBinding<RowEntry>() {
      {
        bind(query.textProperty());
        bind(all.selectedProperty());
        bind(selected.selectedProperty());
        bind(notSelected.selectedProperty());
        bind(listProperty);
      }

      @Override
      protected ObservableList<RowEntry> computeValue() {
        return displayedItems.filtered(rowEntry -> {
          String text = query.getText().toLowerCase();
          String title = ((AbstractUnit) rowEntry.getUnit()).getTitle().toLowerCase();
          CheckBox cb = (CheckBox) rowEntry.getCheckbox();

          if (title.contains(text) || text.isEmpty()) {
            if (listProperty.get().contains(rowEntry)) {
              if (cb.isSelected() && (all.isSelected() || selected.isSelected())) {
                return true;
              }
              if (!cb.isSelected() && (all.isSelected() || notSelected.isSelected())) {
                return true;
              }
            }
          }

          return false;
        });
      }
    };

    units.itemsProperty().bind(binding);
  }

  /**
   * Setter for abstract units. Required to display content.
   * @param abstractUnits List of abstract units to be displayed in TableView
   */
  public void setAbstractUnits(List<AbstractUnit> abstractUnits) {
    abstractUnits.forEach(abstractUnit -> {
      Tooltip tooltip = new Tooltip(abstractUnit.getTitle());
      CheckBox cb = new CheckBox();
      cb.setTooltip(tooltip);
      allItems.add(new RowEntry(cb, abstractUnit));
      allItems();
    });
  }

  public final class RowEntry<T1, T2> {
    private final T1 checkbox;
    private final T2 unit;
    private final String title;

    RowEntry(final T1 checkbox, final T2 unit) {
      this.checkbox = checkbox;
      this.unit = unit;
      this.title = ((AbstractUnit) unit).getTitle();
      ((CheckBox) checkbox).selectedProperty().addListener((observable, oldValue, newValue) -> {
        if (newValue) {
          selectedItems.add(this);
        } else {
          selectedItems.remove(this);
        }
      });
    }

    public T1 getCheckbox() {
      return checkbox;
    }

    public T2 getUnit() {
      return unit;
    }

    public String getTitle() {
      return title;
    }
  }
}

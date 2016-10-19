package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.ui.layout.Inflater;

import java.net.URL;
import java.util.ResourceBundle;

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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

public class AbstractUnitFilter extends VBox implements Initializable {

  private final Delayed<Store> delayedStore;
  private final ToggleGroup filterGroup = new ToggleGroup();
  private ObservableList<RowEntry> allItems = FXCollections.observableArrayList();
  private ObservableList<RowEntry> displayedItems = FXCollections.observableArrayList();
  private ObservableList<RowEntry> selectedItems = FXCollections.observableArrayList();

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
  public AbstractUnitFilter(final Inflater inflater,
                            final Delayed<Store> delayedStore) {
    this.delayedStore = delayedStore;

    inflater.inflate("components/AbstractUnitFilter", this, this, "main");
  }

  private void sortUnitsByName() {
    units.getItems().sort((o1, o2) -> ((AbstractUnit) o1.getUnit()).getTitle()
      .compareTo(((AbstractUnit) o2.getUnit()).getTitle()));
    units.refresh();
  }

  @FXML
  public void search() {
    ObservableList<RowEntry> filtered = FXCollections.observableArrayList(displayedItems);
    query.textProperty().addListener((observable, oldValue, newValue) ->
      filtered.filtered(rowEntry -> {
        if (newValue == null || newValue.isEmpty()) {
          return true;
        }

        if (((AbstractUnit) rowEntry.getUnit()).getTitle().toLowerCase()
          .contains(newValue.toLowerCase())) {
          return true;
        }

        return false;
      }));

    displayedItems.clear();
    displayedItems.addAll(filtered);
    sortUnitsByName();

    all.setSelected(false);
    selected.setSelected(false);
    notSelected.setSelected(false);
  }

  @FXML
  public void allItems() {
    displayedItems.clear();
    displayedItems.addAll(allItems);
    sortUnitsByName();
  }

  @FXML
  public void filterBySelected() {
    displayedItems.clear();
    displayedItems.addAll(selectedItems);
    sortUnitsByName();
  }

  @FXML
  public void filterByUnselected() {
    displayedItems.clear();
    displayedItems.addAll(allItems.filtered(rowEntry -> !selectedItems.contains(rowEntry)));
    sortUnitsByName();
  }

  @FXML
  public void resetSelection() {
    allItems.forEach(rowEntry -> ((CheckBox) rowEntry.getCheckbox()).setSelected(false));
    selectedItems.clear();
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

    TableColumn<RowEntry, CheckBox> checkBoxTableColumn = new TableColumn<>("Check");
    TableColumn<RowEntry, String> nameTableColumn = new TableColumn<>("Abstract Unit Title");

    checkBoxTableColumn.setCellValueFactory(new PropertyValueFactory<>("checkbox"));
    checkBoxTableColumn.setSortable(false);
    checkBoxTableColumn.setResizable(false);
    checkBoxTableColumn.setPrefWidth(50);
    nameTableColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
    nameTableColumn.setSortable(false);
    nameTableColumn.setPrefWidth(1000);
    nameTableColumn.setResizable(false);

    units.getColumns().addAll(checkBoxTableColumn, nameTableColumn);

    delayedStore.whenAvailable(store ->
        store.getAbstractUnits().forEach(abstractUnit -> {
          allItems.add(new RowEntry<>(new CheckBox(), abstractUnit));
          allItems();
          sortUnitsByName();
        }));
    units.setItems(displayedItems);
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

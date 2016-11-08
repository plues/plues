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

@SuppressWarnings("WeakerAccess")
public class AbstractUnitFilter extends VBox implements Initializable {

  private final ToggleGroup filterGroup = new ToggleGroup();
  private final ObservableList<RowEntry> allItems = FXCollections.observableArrayList();
  private final ObservableList<AbstractUnit> selectedItems = FXCollections.observableArrayList();
  private ListBinding<RowEntry> binding;
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

  @FXML
  @SuppressWarnings("unused")
  private TableColumn<RowEntry, String> checkboxColumn;

  @FXML
  @SuppressWarnings("unused")
  private TableColumn<RowEntry, String> abstractUnitTitleColumn;

  @FXML
  @SuppressWarnings("unused")
  private TableColumn<RowEntry, String> abstractUnitKeyColumn;

  @Inject
  public AbstractUnitFilter(final Inflater inflater) {
    inflater.inflate("components/AbstractUnitFilter", this, this, "filter");
  }

  /**
   * OnClick method to remove selection and return to all units view.
   */
  @FXML
  @SuppressWarnings("unused")
  public void resetSelection() {
    allItems.forEach(rowEntry -> rowEntry.getCheckbox().setSelected(false));
    selectedItems.clear();
    query.clear();
    binding.invalidate();

    selected.setSelected(false);
    notSelected.setSelected(false);
    all.setSelected(true);
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    selected.setToggleGroup(filterGroup);
    notSelected.setToggleGroup(filterGroup);
    all.setToggleGroup(filterGroup);

    units.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    listProperty = new SimpleListProperty<>(allItems);

    checkboxColumn.setCellValueFactory(new PropertyValueFactory<>("checkbox"));
    checkboxColumn.setSortable(false);
    checkboxColumn.setResizable(false);

    abstractUnitTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
    abstractUnitTitleColumn.setSortable(false);

    abstractUnitKeyColumn.setCellValueFactory(new PropertyValueFactory<>("key"));
    abstractUnitKeyColumn.setSortable(false);
  }

  /**
   * Setter for abstract units. Required to display content.
   * @param abstractUnits List of abstract units to be displayed in TableView
   */
  void setAbstractUnits(final List<AbstractUnit> abstractUnits) {
    abstractUnits.forEach(abstractUnit -> allItems.add(getTableViewItem(abstractUnit)));

    units.itemsProperty().unbind();
    binding = new ListBinding<RowEntry>() {
      {
        listProperty.get().forEach(rowEntry -> bind(rowEntry.getCheckbox().selectedProperty()));
        bind(query.textProperty(), all.selectedProperty(), selected.selectedProperty(),
            notSelected.selectedProperty(), listProperty);
      }

      @Override
      protected ObservableList<RowEntry> computeValue() {
        return listProperty.get().filtered(rowEntry -> rowEntry.matches(query, all.isSelected(),
          selected.isSelected(), notSelected.isSelected()));
      }
    };
    units.itemsProperty().bind(binding);
  }

  private RowEntry getTableViewItem(final AbstractUnit unit) {
    final CheckBox checkBox = new CheckBox();
    final Tooltip tooltip = new Tooltip(unit.getTitle());
    checkBox.setTooltip(tooltip);
    checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue) {
        selectedItems.add(unit);
      } else {
        selectedItems.remove(unit);
      }
    });

    return new RowEntry(checkBox, unit.getTitle(), unit.getKey());
  }

  @SuppressWarnings("WeakerAccess")
  public static final class RowEntry {
    private final CheckBox checkbox;
    private final String title;
    private final String key;

    RowEntry(final CheckBox checkbox, final String title, final String key) {
      this.checkbox = checkbox;
      this.title = title;
      this.key = key;
    }

    public CheckBox getCheckbox() {
      return checkbox;
    }

    public String getTitle() {
      return this.title;
    }

    public String getKey() {
      return key;
    }

    boolean matches(final TextField query, final boolean all, final boolean showSelected,
        final boolean showNotSelected) {
      return this.titleMatchesQuery(query)
        && this.checkboxMatchesCriteria(all, showSelected, showNotSelected);
    }

    private boolean checkboxMatchesCriteria(final boolean all, final boolean showSelected,
        final boolean showNotSelected) {
      final boolean checked = this.checkbox.isSelected();
      final boolean showIfSelected = checked && showSelected;
      final boolean showIfNotSelected = !checked && showNotSelected;
      return all || showIfSelected || showIfNotSelected;
    }

    private boolean titleMatchesQuery(final TextField query) {
      final String lowerCaseTitle = title.toLowerCase();
      final String lowerCaseKey = key.toLowerCase();
      final String text = query.getText().toLowerCase();
      return text.isEmpty() || lowerCaseTitle.contains(text) || lowerCaseKey.contains(text);
    }
  }
}

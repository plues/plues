package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class ModuleAbstractUnitUnitSemesterConflicts extends VBox implements Initializable {

  private SimpleListProperty<Module> modules;
  private SimpleListProperty<TableRowTriple> entries;
  private Map<Module, List<TableRowTriple>> moduleAbstractUnitUnitSemesterConflicts;

  @FXML
  @SuppressWarnings("unused")
  private ListView<Module> listViewModules;
  @FXML
  @SuppressWarnings("unused")
  private TableView<TableRowTriple> tableViewAbstractUnitUnitSemesters;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableRowTriple, String> tableColumnAbstractUnit;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableRowTriple, String> tableColumnUnit;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<TableRowTriple, String> tableColumnSemesters;

  @Inject
  public ModuleAbstractUnitUnitSemesterConflicts(final Inflater inflater) {
    modules = new SimpleListProperty<>(FXCollections.observableArrayList());
    entries = new SimpleListProperty<>(FXCollections.observableArrayList());

    inflater.inflate("/components/reports/ModuleAbstractUnitUnitSemesterConflicts",
        this, this, "reports");
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    tableViewAbstractUnitUnitSemesters.itemsProperty().bind(entries);
    tableColumnAbstractUnit.setCellValueFactory(new PropertyValueFactory<>("abstractUnitTitle"));
    tableColumnUnit.setCellValueFactory(new PropertyValueFactory<>("unitTitle"));
    tableColumnSemesters.setCellValueFactory(new PropertyValueFactory<>("semesters"));

    listViewModules.itemsProperty().bind(modules);
    listViewModules.setCellFactory(param -> new ListCell<Module>() {
      @Override
      protected void updateItem(Module module, boolean empty) {
        super.updateItem(module, empty);
        if (!empty) {
          setText(module.getTitle());
        }
      }
    });
    listViewModules.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) ->
          entries.setAll(moduleAbstractUnitUnitSemesterConflicts.get(newValue)));
  }

  public void setData(final Map<Module, List<TableRowTriple>> moduleAbstractUnitUnitSemesterConflicts) {
    this.moduleAbstractUnitUnitSemesterConflicts = moduleAbstractUnitUnitSemesterConflicts;
    this.modules.setAll(moduleAbstractUnitUnitSemesterConflicts.keySet());
  }

  public static final class TableRowTriple {
    private final String abstractUnitTitle;
    private final String unitTitle;
    private final String semesters;

    /**
     * An object to obtain three values of the same type to use within a table view.
     */
    public TableRowTriple(final AbstractUnit abstractUnit, final Unit unit, final String semesters) {
      this.abstractUnitTitle = abstractUnit.getTitle();
      this.unitTitle = unit.getTitle();
      this.semesters = semesters;
    }

    @SuppressWarnings("unused")
    public String getAbstractUnitTitle() {
      return this.abstractUnitTitle;
    }

    @SuppressWarnings("unused")
    public String getUnitTitle() {
      return this.unitTitle;
    }

    @SuppressWarnings("unused")
    public String getSemesters() {
      return this.semesters;
    }

  }
}

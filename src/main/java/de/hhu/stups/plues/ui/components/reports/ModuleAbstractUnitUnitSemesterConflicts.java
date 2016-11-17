package de.hhu.stups.plues.ui.components.reports;

import com.google.common.base.Joiner;
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
import java.util.Set;

public class ModuleAbstractUnitUnitSemesterConflicts extends VBox implements Initializable {

  private final SimpleListProperty<Module> modules;
  private final SimpleListProperty<Conflict> entries;
  private Map<Module, List<Conflict>> moduleAbstractUnitUnitSemesterConflicts;

  @FXML
  private ListView<Module> listViewModules;
  @FXML
  private TableView<Conflict> tableViewAbstractUnitUnitSemesters;
  @FXML
  private TableColumn<Conflict, String> tableColumnAbstractUnitKey;
  @FXML
  private TableColumn<Conflict, String> tableColumnAbstractUnitTitle;
  @FXML
  private TableColumn<Conflict, String> tableColumnAbstractUnitSemesters;
  @FXML
  private TableColumn<Conflict, String> tableColumnUnitTitle;
  @FXML
  private TableColumn<Conflict, String> tableColumnUnitKey;
  @FXML
  private TableColumn<Conflict, String> tableColumnUnitSemesters;

  /**
   * Default constructor.
   *
   * @param inflater Handle fxml and resources
   */
  @Inject
  public ModuleAbstractUnitUnitSemesterConflicts(final Inflater inflater) {
    modules = new SimpleListProperty<>(FXCollections.observableArrayList());
    entries = new SimpleListProperty<>(FXCollections.observableArrayList());

    inflater.inflate("/components/reports/ModuleAbstractUnitUnitSemesterConflicts",
        this, this, "reports");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    tableViewAbstractUnitUnitSemesters.itemsProperty().bind(entries);

    tableColumnAbstractUnitKey.setCellValueFactory(new PropertyValueFactory<>("abstractUnitKey"));
    tableColumnAbstractUnitTitle.setCellValueFactory(
        new PropertyValueFactory<>("abstractUnitTitle"));
    tableColumnAbstractUnitSemesters.setCellValueFactory(
        new PropertyValueFactory<>("abstractUnitSemesters"));

    tableColumnUnitKey.setCellValueFactory(new PropertyValueFactory<>("unitKey"));
    tableColumnUnitTitle.setCellValueFactory(new PropertyValueFactory<>("unitTitle"));
    tableColumnUnitSemesters.setCellValueFactory(new PropertyValueFactory<>("unitSemesters"));

    listViewModules.itemsProperty().bind(modules);
    listViewModules.setCellFactory(param -> new ListCell<Module>() {
      @Override
      protected void updateItem(final Module module, final boolean empty) {
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

  public void setData(final Map<Module, List<Conflict>>
                        moduleAbstractUnitUnitSemesterConflicts) {
    this.moduleAbstractUnitUnitSemesterConflicts = moduleAbstractUnitUnitSemesterConflicts;
    this.modules.setAll(moduleAbstractUnitUnitSemesterConflicts.keySet());
  }

  public static final class Conflict {
    private final Set<Integer> semesters;
    private final AbstractUnit abstractUnit;
    private final Unit unit;

    /**
     * An object to obtain three values of the same type to use within a table view.
     */
    public Conflict(final AbstractUnit abstractUnit, final Unit unit,
                    final Set<Integer> abstractUnitSemesters) {
      this.abstractUnit = abstractUnit;
      this.unit = unit;
      this.semesters = abstractUnitSemesters;
    }

    @SuppressWarnings("unused")
    public String getAbstractUnitTitle() {
      return this.abstractUnit.getTitle();
    }

    @SuppressWarnings("unused")
    public String getAbstractUnitKey() {
      return this.abstractUnit.getKey();
    }

    @SuppressWarnings("unused")
    public String getAbstractUnitSemesters() {
      return Joiner.on(",").join(this.semesters);
    }

    @SuppressWarnings("unused")
    public String getUnitSemesters() {
      return Joiner.on(",").join(unit.getSemesters());
    }

    @SuppressWarnings("unused")
    public String getUnitKey() {
      return unit.getKey();
    }

    @SuppressWarnings("unused")
    public String getUnitTitle() {
      return this.unit.getTitle();
    }
  }
}

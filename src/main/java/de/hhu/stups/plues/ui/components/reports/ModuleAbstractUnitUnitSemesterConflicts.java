package de.hhu.stups.plues.ui.components.reports;

import com.google.common.base.Joiner;
import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.ListBinding;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

public class ModuleAbstractUnitUnitSemesterConflicts extends VBox implements Initializable {

  private final SimpleListProperty<Module> modules;
  private final ObservableMap<Module, List<Conflict>> moduleAbstractUnitUnitSemesterConflictsMap;

  @FXML
  @SuppressWarnings("unused")
  private TableView<Module> tableViewModules;
  @FXML
  @SuppressWarnings("unused")
  private TableView<Conflict> tableViewAbstractUnitUnitSemesters;
  @FXML
  @SuppressWarnings("unused")
  private Text txtExplanation;

  /**
   * Default constructor.
   *
   * @param inflater Handle fxml and resources
   */
  @Inject
  public ModuleAbstractUnitUnitSemesterConflicts(final Inflater inflater) {
    modules = new SimpleListProperty<>(FXCollections.observableArrayList());
    moduleAbstractUnitUnitSemesterConflictsMap = FXCollections.observableHashMap();

    inflater.inflate("components/reports/ModuleAbstractUnitUnitSemesterConflicts",
        this, this, "reports", "Column");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    tableViewAbstractUnitUnitSemesters.itemsProperty().bind(new ListBinding<Conflict>() {
      {
        bind(tableViewModules.getSelectionModel().selectedItemProperty());
      }

      @Override
      protected ObservableList<Conflict> computeValue() {
        final Module module = tableViewModules.getSelectionModel().getSelectedItem();
        return FXCollections.observableList(moduleAbstractUnitUnitSemesterConflictsMap
            .getOrDefault(module, Collections.emptyList()));
      }
    });

    tableViewModules.itemsProperty().bind(modules);

    txtExplanation.wrappingWidthProperty().bind(tableViewModules.widthProperty().subtract(25.0));
  }

  public void setData(final Map<Module, List<Conflict>> moduleAbstractUnitUnitSemesterConflicts) {
    this.moduleAbstractUnitUnitSemesterConflictsMap.putAll(moduleAbstractUnitUnitSemesterConflicts);
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

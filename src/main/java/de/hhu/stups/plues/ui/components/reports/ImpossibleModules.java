package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.ui.layout.Inflater;
import javafx.beans.binding.ListBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ImpossibleModules extends VBox implements Initializable {

  private final SimpleListProperty<Module> incompleteModules;
  private final SimpleListProperty<Module> impossibleModulesBecauseOfMissingElectiveAbstractUnits;

  @FXML
  @SuppressWarnings("unused")
  private ToggleButton buttonIncompleteModules;
  @FXML
  @SuppressWarnings("unused")
  private ToggleButton buttonMissingElectiveAbstractUnits;

  @FXML
  @SuppressWarnings("unused")
  private Label explanation;
  @FXML
  @SuppressWarnings("unused")
  private TableView<Module> tableViewModules;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, Integer> columnModulePordnr;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> columnModuleTitle;

  /**
   * Default constructor for incomplete modules component.
   * @param inflater Inflater to handle fxml files and resources
   */
  @Inject
  public ImpossibleModules(final Inflater inflater) {
    incompleteModules = new SimpleListProperty<>(FXCollections.observableArrayList());
    impossibleModulesBecauseOfMissingElectiveAbstractUnits = new SimpleListProperty<>(
      FXCollections.observableArrayList());

    inflater.inflate("/components/reports/ImpossibleModules", this, this, "reports");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    final ListBinding<Module> binding = new ListBinding<Module>() {
      {
        bind(buttonIncompleteModules.selectedProperty());
        bind(buttonMissingElectiveAbstractUnits.selectedProperty());
        bind(incompleteModules);
        bind(impossibleModulesBecauseOfMissingElectiveAbstractUnits);
      }

      @Override
      protected ObservableList<Module> computeValue() {
        if (buttonIncompleteModules.isSelected()) {
          return incompleteModules;
        } else {
          if (buttonMissingElectiveAbstractUnits.isSelected()) {
            return impossibleModulesBecauseOfMissingElectiveAbstractUnits;
          } else {
            return null;
          }
        }
      }
    };
    tableViewModules.itemsProperty().bind(binding);

    final StringBinding stringBinding = new StringBinding() {
      {
        bind(buttonIncompleteModules.selectedProperty());
        bind(buttonMissingElectiveAbstractUnits.selectedProperty());
      }

      @Override
      protected String computeValue() {
        final String string;
        if (buttonIncompleteModules.isSelected()) {
          string = resources.getString("explainIncompleteModules");
        } else {
          if (buttonMissingElectiveAbstractUnits.isSelected()) {
            string = resources.getString(
                "explainImpossibleModulesBecauseOfMissingElectiveAbstractUnits");
          } else {
            string = null;
          }
        }

        return string;
      }
    };
    explanation.textProperty().bind(stringBinding);

    columnModulePordnr.setCellValueFactory(new PropertyValueFactory<>("pordnr"));
    columnModuleTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
  }

  /**
   * Set data for this component.
   * @param incompleteModules Incomplete modules
   * @param impossibleModulesBecauseOfMissingElectiveAbstractUnits modules which are impossible
   *                                                               because of missing elective
   *                                                               abstract units
   */
  public void setData(final List<Module> incompleteModules,
                      final List<Module> impossibleModulesBecauseOfMissingElectiveAbstractUnits) {
    this.incompleteModules.setAll(incompleteModules);
    this.impossibleModulesBecauseOfMissingElectiveAbstractUnits.setAll(
        impossibleModulesBecauseOfMissingElectiveAbstractUnits);
  }
}

package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.ui.components.detailview.DetailViewHelper;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListBinding;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.controlsfx.control.SegmentedButton;

import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

public class ImpossibleModules extends VBox {

  private final SimpleListProperty<Module> incompleteModules;
  private final SimpleListProperty<Module> impossibleModulesBecauseOfMissingElectiveAbstractUnits;
  private final SimpleMapProperty<Module, Set<AbstractUnit>>
      impossibleModulesBecauseOfIncompleteQuasiMandatoryAbstractUnits;
  private final Router router;

  @FXML
  private ResourceBundle resources;

  @FXML
  @SuppressWarnings("unused")
  private SegmentedButton segmentedButtons;
  @FXML
  @SuppressWarnings("unused")
  private ToggleButton buttonIncompleteModules;
  @FXML
  @SuppressWarnings("unused")
  private ToggleButton buttonMissingElectiveAbstractUnits;
  @FXML
  @SuppressWarnings("unused")
  private ToggleButton buttonIncompleteQuasiMandatoryAbstractUnits;
  @FXML
  @SuppressWarnings("unused")
  private Text txtExplanation;
  @FXML
  @SuppressWarnings("unused")
  private TableView<Module> tableViewModules;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Course, String> tableColumnModulePordnr;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Course, String> tableColumnModuleTitle;
  @FXML
  @SuppressWarnings("unused")
  private TableView<AbstractUnit> tableViewIncompleteAbstractUnits;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> tableColumnAbstractUnitKey;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> tableColumnAbstractUnitTitle;

  /**
   * Default constructor for incomplete modules component.
   *
   * @param inflater Inflater to handle fxml files and resources
   * @param router   Router.
   */
  @Inject
  public ImpossibleModules(final Inflater inflater, final Router router) {
    this.router = router;
    this.incompleteModules = new SimpleListProperty<>(FXCollections.observableArrayList());
    this.impossibleModulesBecauseOfMissingElectiveAbstractUnits
        = new SimpleListProperty<>(FXCollections.observableArrayList());
    this.impossibleModulesBecauseOfIncompleteQuasiMandatoryAbstractUnits =
        new SimpleMapProperty<>();
    inflater.inflate("components/reports/ImpossibleModules", this, this, "reports", "Column");
  }

  @FXML
  public void initialize(final ResourceBundle resources) {
    getChildren().remove(tableViewIncompleteAbstractUnits);
    segmentedButtons.setToggleGroup(new PersistentToggleGroup());

    tableViewModules.itemsProperty().bind(new ModuleListBinding());

    tableViewModules.setOnMouseClicked(
        DetailViewHelper.getModuleMouseHandler(tableViewModules, router));

    tableViewIncompleteAbstractUnits.setOnMouseClicked(
        DetailViewHelper.getAbstractUnitMouseHandler(tableViewIncompleteAbstractUnits, router));

    txtExplanation.textProperty().bind(
        Bindings.createStringBinding(() -> getExplanation(resources),
            buttonIncompleteModules.selectedProperty(),
            buttonMissingElectiveAbstractUnits.selectedProperty(),
            buttonIncompleteQuasiMandatoryAbstractUnits.selectedProperty()));
    txtExplanation.wrappingWidthProperty().bind(tableViewModules.widthProperty().subtract(25.0));

    buttonIncompleteQuasiMandatoryAbstractUnits.selectedProperty().addListener(
        (observable, oldValue, newValue) -> {
          if (!newValue) {
            getChildren().remove(tableViewIncompleteAbstractUnits);
            return;
          }
          if (!getChildren().contains(tableViewIncompleteAbstractUnits)) {
            getChildren().add(tableViewIncompleteAbstractUnits);
          }
          tableViewModules.getSelectionModel().selectFirst();
        });

    tableViewModules.getSelectionModel().selectedItemProperty().addListener(
        (observable, oldValue, newValue) -> {
          if (!buttonIncompleteQuasiMandatoryAbstractUnits.isSelected()) {
            return;
          }
          tableViewIncompleteAbstractUnits.itemsProperty().set(FXCollections.observableArrayList(
              impossibleModulesBecauseOfIncompleteQuasiMandatoryAbstractUnits.get().get(newValue)));
        });
  }

  private String getExplanation(final ResourceBundle resources) {
    if (buttonIncompleteModules.isSelected()) {
      return resources.getString("explain.IncompleteModules");
    }
    if (buttonMissingElectiveAbstractUnits.isSelected()) {
      return resources.getString(
          "explain.ImpossibleModulesBecauseOfMissingElectiveAbstractUnits");
    }
    if (buttonIncompleteQuasiMandatoryAbstractUnits.isSelected()) {
      return resources.getString(
          "explain.ImpossibleModulesBecauseOfIncompleteQuasiMandatoryAbstractUnits");
    }

    return null;
  }

  /**
   * Set data for this component.
   */
  public void setData(final List<Module> incompleteModules,
                      final List<Module> impossibleModulesBecauseOfMissingElectiveAbstractUnits,
                      final Map<Module, Set<AbstractUnit>>
                          impossibleModulesBecauseOfIncompleteQuasiMandatoryAbstractUnits) {
    this.incompleteModules.setAll(incompleteModules);
    this.impossibleModulesBecauseOfMissingElectiveAbstractUnits.setAll(
        impossibleModulesBecauseOfMissingElectiveAbstractUnits);
    this.impossibleModulesBecauseOfIncompleteQuasiMandatoryAbstractUnits.set(
        FXCollections.observableMap(
            impossibleModulesBecauseOfIncompleteQuasiMandatoryAbstractUnits));
  }

  private class ModuleListBinding extends ListBinding<Module> {

    ModuleListBinding() {
      bind(buttonIncompleteModules.selectedProperty());
      bind(buttonMissingElectiveAbstractUnits.selectedProperty());
      bind(buttonIncompleteQuasiMandatoryAbstractUnits.selectedProperty());
      bind(incompleteModules);
      bind(impossibleModulesBecauseOfMissingElectiveAbstractUnits);
      bind(impossibleModulesBecauseOfIncompleteQuasiMandatoryAbstractUnits);
    }

    @Override
    protected ObservableList<Module> computeValue() {
      if (buttonIncompleteModules.isSelected()) {
        return incompleteModules;
      }

      if (buttonMissingElectiveAbstractUnits.isSelected()) {
        return impossibleModulesBecauseOfMissingElectiveAbstractUnits;
      }

      if (buttonIncompleteQuasiMandatoryAbstractUnits.isSelected()) {
        return FXCollections.observableArrayList(
            impossibleModulesBecauseOfIncompleteQuasiMandatoryAbstractUnits.keySet());
      }

      return null;
    }
  }
}

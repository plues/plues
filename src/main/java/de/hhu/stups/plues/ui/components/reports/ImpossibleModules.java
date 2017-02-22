package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.ui.components.detailview.DetailViewHelper;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListBinding;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.controlsfx.control.SegmentedButton;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ImpossibleModules extends VBox implements Initializable {

  private final SimpleListProperty<Module> incompleteModules;
  private final SimpleListProperty<Module> impossibleModulesBecauseOfMissingElectiveAbstractUnits;
  private final Router router;

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

  /**
   * Default constructor for incomplete modules component.
   *
   * @param inflater Inflater to handle fxml files and resources
   * @param router Router.
   */
  @Inject
  public ImpossibleModules(final Inflater inflater, final Router router) {
    this.router = router;
    this.incompleteModules = new SimpleListProperty<>(FXCollections.observableArrayList());
    this.impossibleModulesBecauseOfMissingElectiveAbstractUnits
        = new SimpleListProperty<>(FXCollections.observableArrayList());

    inflater.inflate("components/reports/ImpossibleModules", this, this, "reports", "Column");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    segmentedButtons.setToggleGroup(new PersistentToggleGroup());

    tableViewModules.itemsProperty().bind(new ModuleListBinding());

    tableViewModules.setOnMouseClicked(
        DetailViewHelper.getModuleMouseHandler(tableViewModules, router));

    txtExplanation.textProperty().bind(
        Bindings.createStringBinding(() -> getExplanation(resources),
            buttonIncompleteModules.selectedProperty(),
            buttonMissingElectiveAbstractUnits.selectedProperty()));
    txtExplanation.wrappingWidthProperty().bind(tableViewModules.widthProperty().subtract(25.0));

    bindTableColumnsWidth();
  }

  private String getExplanation(final ResourceBundle resources) {
    if (buttonIncompleteModules.isSelected()) {
      return resources.getString("explain.IncompleteModules");
    }
    if (buttonMissingElectiveAbstractUnits.isSelected()) {
      return resources.getString(
        "explain.ImpossibleModulesBecauseOfMissingElectiveAbstractUnits");
    }

    return null;
  }

  private void bindTableColumnsWidth() {
    tableColumnModulePordnr.prefWidthProperty().bind(
        tableViewModules.widthProperty().multiply(0.2));
    tableColumnModuleTitle.prefWidthProperty().bind(
        tableViewModules.widthProperty().multiply(0.76));
  }

  /**
   * Set data for this component.
   */
  public void setData(final List<Module> incompleteModules,
                      final List<Module> impossibleModulesBecauseOfMissingElectiveAbstractUnits) {
    this.incompleteModules.setAll(incompleteModules);
    this.impossibleModulesBecauseOfMissingElectiveAbstractUnits.setAll(
        impossibleModulesBecauseOfMissingElectiveAbstractUnits);
  }

  private class ModuleListBinding extends ListBinding<Module> {

    ModuleListBinding() {
      bind(buttonIncompleteModules.selectedProperty());
      bind(buttonMissingElectiveAbstractUnits.selectedProperty());
      bind(incompleteModules);
      bind(impossibleModulesBecauseOfMissingElectiveAbstractUnits);
    }

    @Override
    protected ObservableList<Module> computeValue() {
      if (buttonIncompleteModules.isSelected()) {
        return incompleteModules;
      }

      if (buttonMissingElectiveAbstractUnits.isSelected()) {
        return impossibleModulesBecauseOfMissingElectiveAbstractUnits;
      }

      return null;
    }
  }
}

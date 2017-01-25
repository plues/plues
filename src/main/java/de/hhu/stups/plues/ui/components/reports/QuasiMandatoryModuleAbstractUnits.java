package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

public class QuasiMandatoryModuleAbstractUnits extends VBox implements Initializable {

  private Map<Module, Set<AbstractUnit>> quasiMandatoryModuleAbstractUnitsMap;
  private final SimpleListProperty<Module> modules;
  private final SimpleListProperty<AbstractUnit> abstractUnits;

  @FXML
  @SuppressWarnings("unused")
  private TableView<Module> tableViewQuasiMandatoryModules;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> tableColumnModulePordnr;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> tableColumnModuleTitle;
  @FXML
  @SuppressWarnings("unused")
  private TableView<AbstractUnit> tableViewAbstractUnits;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> tableColumnAbstractUnitKey;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> tableColumnAbstractUnitTitle;
  @FXML
  @SuppressWarnings("unused")
  private Text txtExplanation;

  /**
   * Default constructor.
   *
   * @param inflater Handle fxml and resources
   */
  @Inject
  public QuasiMandatoryModuleAbstractUnits(final Inflater inflater) {
    abstractUnits = new SimpleListProperty<>(FXCollections.observableArrayList());
    modules = new SimpleListProperty<>(FXCollections.observableArrayList());

    inflater.inflate("components/reports/QuasiMandatoryModuleAbstractUnits",
        this, this, "reports", "Column");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    tableViewQuasiMandatoryModules.itemsProperty().bind(modules);
    tableViewAbstractUnits.itemsProperty().bind(abstractUnits);

    tableViewQuasiMandatoryModules.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) ->
            abstractUnits.setAll(quasiMandatoryModuleAbstractUnitsMap.get(newValue)));

    txtExplanation.wrappingWidthProperty().bind(
        tableViewAbstractUnits.widthProperty().subtract(25.0));

    bindTableColumnsWidth();
  }

  private void bindTableColumnsWidth() {
    tableColumnModulePordnr.prefWidthProperty().bind(
        tableViewQuasiMandatoryModules.widthProperty().multiply(0.2));
    tableColumnModuleTitle.prefWidthProperty().bind(
        tableViewQuasiMandatoryModules.widthProperty().multiply(0.76));

    tableColumnAbstractUnitKey.prefWidthProperty().bind(
        tableViewAbstractUnits.widthProperty().multiply(0.2));
    tableColumnAbstractUnitTitle.prefWidthProperty().bind(
        tableViewAbstractUnits.widthProperty().multiply(0.76));
  }

  public void setData(final Map<Module, Set<AbstractUnit>> quasiMandatoryModuleAbstractUnitsMap) {
    this.quasiMandatoryModuleAbstractUnitsMap = quasiMandatoryModuleAbstractUnitsMap;
    modules.addAll(quasiMandatoryModuleAbstractUnitsMap.keySet());
  }
}

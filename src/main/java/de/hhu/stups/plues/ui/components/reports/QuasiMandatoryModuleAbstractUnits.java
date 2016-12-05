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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

public class QuasiMandatoryModuleAbstractUnits extends VBox implements Initializable {

  private Map<Module, Set<AbstractUnit>> quasiMandatoryModuleAbstractUnits;
  private final SimpleListProperty<Module> modules;
  private final SimpleListProperty<AbstractUnit> abstractUnits;

  @FXML
  @SuppressWarnings("unused")
  private TableView<Module> tableViewQuasiMandatoryModules;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, Integer> columnModulePordnr;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> columnModuleTitle;
  @FXML
  @SuppressWarnings("unused")
  private TableView<AbstractUnit> tableViewAbstractUnits;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<AbstractUnit, String> columnAbstractUnitKey;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<AbstractUnit, String> columnAbstractUnitTitle;

  /**
   * Default constructor.
   * @param inflater Handle fxml and resources
   */
  @Inject
  public QuasiMandatoryModuleAbstractUnits(final Inflater inflater) {
    abstractUnits = new SimpleListProperty<>(FXCollections.observableArrayList());
    modules = new SimpleListProperty<>(FXCollections.observableArrayList());

    inflater.inflate("/components/reports/QuasiMandatoryModuleAbstractUnits",
        this, this, "reports", "Column");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    tableViewQuasiMandatoryModules.itemsProperty().bind(modules);
    columnModulePordnr.setCellValueFactory(new PropertyValueFactory<>("pordnr"));
    columnModuleTitle.setCellValueFactory(new PropertyValueFactory<>("title"));

    tableViewAbstractUnits.itemsProperty().bind(abstractUnits);
    columnAbstractUnitKey.setCellValueFactory(new PropertyValueFactory<>("key"));
    columnAbstractUnitTitle.setCellValueFactory(new PropertyValueFactory<>("title"));

    tableViewQuasiMandatoryModules.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) ->
          abstractUnits.setAll(quasiMandatoryModuleAbstractUnits.get(newValue)));
  }

  public void setData(final Map<Module, Set<AbstractUnit>> quasiMandatoryModuleAbstractUnits) {
    this.quasiMandatoryModuleAbstractUnits = quasiMandatoryModuleAbstractUnits;
    modules.addAll(quasiMandatoryModuleAbstractUnits.keySet());
  }
}

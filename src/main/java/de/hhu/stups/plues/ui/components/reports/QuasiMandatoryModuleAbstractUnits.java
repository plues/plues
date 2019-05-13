package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.ui.components.detailview.DetailViewHelper;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.Map;
import java.util.Set;

public class QuasiMandatoryModuleAbstractUnits extends VBox {

  private Map<Module, Set<AbstractUnit>> quasiMandatoryModuleAbstractUnitsMap;
  private final SimpleListProperty<Module> modules;
  private final SimpleListProperty<AbstractUnit> abstractUnits;
  private final Router router;

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
  public QuasiMandatoryModuleAbstractUnits(final Inflater inflater, final Router router) {
    this.router = router;
    this.abstractUnits = new SimpleListProperty<>(FXCollections.observableArrayList());
    this.modules = new SimpleListProperty<>(FXCollections.observableArrayList());

    inflater.inflate("components/reports/QuasiMandatoryModuleAbstractUnits",
        this, this, "reports", "Column");
  }

  @FXML
  public void initialize() {
    tableViewQuasiMandatoryModules.itemsProperty().bind(modules);
    tableViewAbstractUnits.itemsProperty().bind(abstractUnits);
    tableViewAbstractUnits.setOnMouseClicked(
        DetailViewHelper.getAbstractUnitMouseHandler(tableViewAbstractUnits, router));
    tableViewQuasiMandatoryModules.setOnMouseClicked(
        DetailViewHelper.getModuleMouseHandler(tableViewQuasiMandatoryModules, router));


    tableViewQuasiMandatoryModules.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) -> {
          if (quasiMandatoryModuleAbstractUnitsMap != null) {
            abstractUnits.setAll(quasiMandatoryModuleAbstractUnitsMap.get(newValue));
          }
        });

    txtExplanation.wrappingWidthProperty().bind(
        tableViewAbstractUnits.widthProperty().subtract(25.0));
  }

  public void setData(final Map<Module, Set<AbstractUnit>> quasiMandatoryModuleAbstractUnitsMap) {
    this.quasiMandatoryModuleAbstractUnitsMap = quasiMandatoryModuleAbstractUnitsMap;
    modules.addAll(quasiMandatoryModuleAbstractUnitsMap.keySet());
  }
}

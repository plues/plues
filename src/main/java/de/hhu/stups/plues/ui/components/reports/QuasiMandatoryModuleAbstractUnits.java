package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

public class QuasiMandatoryModuleAbstractUnits extends VBox implements Initializable {

  private Map<Module, Set<AbstractUnit>> quasiMandatoryModuleAbstractUnits;
  private SimpleListProperty<Module> modules;
  private SimpleListProperty<AbstractUnit> abstractUnits;

  @FXML
  @SuppressWarnings("unused")
  private ListView<Module> listViewQuasiMandatoryModules;
  @FXML
  @SuppressWarnings("unused")
  private ListView<AbstractUnit> listViewAbstractUnits;

  /**
   * Default constructor.
   * @param inflater Handle fxml and resources
   */
  @Inject
  public QuasiMandatoryModuleAbstractUnits(final Inflater inflater) {
    abstractUnits = new SimpleListProperty<>(FXCollections.observableArrayList());
    modules = new SimpleListProperty<>(FXCollections.observableArrayList());

    inflater.inflate("/components/reports/QuasiMandatoryModuleAbstractUnits",
        this, this, "reports");
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    listViewQuasiMandatoryModules.itemsProperty().bind(modules);
    listViewQuasiMandatoryModules.setCellFactory(param -> new ListCell<Module>() {
      @Override
      protected void updateItem(Module module, boolean empty) {
        super.updateItem(module, empty);
        if (!empty) {
          setText(module.getTitle());
        }
      }
    });
    listViewAbstractUnits.itemsProperty().bind(abstractUnits);
    listViewAbstractUnits.setCellFactory(param -> new ListCell<AbstractUnit>() {
      @Override
      protected void updateItem(AbstractUnit abstractUnit, boolean empty) {
        super.updateItem(abstractUnit, empty);
        if (!empty) {
          setText(abstractUnit.getTitle());
        }
      }
    });
    listViewQuasiMandatoryModules.getSelectionModel().selectedItemProperty()
        .addListener((observable, oldValue, newValue) ->
          abstractUnits.setAll(quasiMandatoryModuleAbstractUnits.get(newValue)));
  }

  public void setData(final Map<Module, Set<AbstractUnit>> quasiMandatoryModuleAbstractUnits) {
    this.quasiMandatoryModuleAbstractUnits = quasiMandatoryModuleAbstractUnits;
    modules.addAll(quasiMandatoryModuleAbstractUnits.keySet());
  }
}

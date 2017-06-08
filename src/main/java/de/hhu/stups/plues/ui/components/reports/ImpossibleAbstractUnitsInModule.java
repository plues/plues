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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

public class ImpossibleAbstractUnitsInModule extends VBox implements Initializable {

  private Map<Module, Set<AbstractUnit>> impossibleAbstractUnitsInModuleMap;
  private final SimpleListProperty<Module> modules;
  private final SimpleListProperty<AbstractUnit> abstractUnits;

  @FXML
  @SuppressWarnings("unused")
  private ListView<Module> listViewModules;
  @FXML
  @SuppressWarnings("unused")
  private TableView<AbstractUnit> tableViewAbstractUnits;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<AbstractUnit, String> tableColumnAbstractUnitKey;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<AbstractUnit, String> tableColumnAbstractUnitTitle;
  @FXML
  @SuppressWarnings("unused")
  private Text txtExplanation;

  /**
   * Default constructor
   *
   * @param inflater Inflater to handle fxml files and resources.
   */
  @Inject
  public ImpossibleAbstractUnitsInModule(final Inflater inflater) {
    modules = new SimpleListProperty<>(FXCollections.observableArrayList());
    abstractUnits = new SimpleListProperty<>(FXCollections.observableArrayList());

    inflater.inflate("/components/reports/ImpossibleAbstractUnitsInModule",
        this, this, "reports", "Column");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    tableViewAbstractUnits.itemsProperty().bind(abstractUnits);

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
            abstractUnits.setAll(impossibleAbstractUnitsInModuleMap.get(newValue)));

    txtExplanation.wrappingWidthProperty().bind(
        tableViewAbstractUnits.widthProperty().subtract(25.0));
  }

  public void setData(final Map<Module, Set<AbstractUnit>> impossibleAbstractUnitsInModuleMap) {
    this.impossibleAbstractUnitsInModuleMap = impossibleAbstractUnitsInModuleMap;
    modules.addAll(impossibleAbstractUnitsInModuleMap.keySet());
  }
}

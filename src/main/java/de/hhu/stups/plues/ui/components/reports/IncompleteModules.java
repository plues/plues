package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Set;

public class IncompleteModules extends VBox implements Initializable {

  @FXML
  @SuppressWarnings("unused")
  private TableView<Module> tableViewModules;

  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, Integer> columnModulePordnr;

  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> columnModuleTitle;

  private final Delayed<Store> delayedStore;
  private Store store;

  /**
   * Default constructor for incomplete modules component.
   * @param inflater Inflater to handle fxml files and resources
   * @param delayedStore Store to get necessary information
   */
  @Inject
  public IncompleteModules(final Inflater inflater,
                           final Delayed<Store> delayedStore) {
    this.delayedStore = delayedStore;

    inflater.inflate("/components/reports/IncompleteModules", this, this, "reports");
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    columnModulePordnr.setCellValueFactory(new PropertyValueFactory<>("pordnr"));
    columnModuleTitle.setCellValueFactory(new PropertyValueFactory<>("title"));

    delayedStore.whenAvailable(store -> this.store = store);
  }

  public void setData(final Set<Integer> incompleteModules) {
    incompleteModules.forEach(id -> tableViewModules.getItems().add(store.getModuleById(id)));
  }
}

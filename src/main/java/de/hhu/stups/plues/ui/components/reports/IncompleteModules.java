package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

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

  /**
   * Default constructor for incomplete modules component.
   * @param inflater Inflater to handle fxml files and resources
   */
  @Inject
  public IncompleteModules(final Inflater inflater) {
    inflater.inflate("/components/reports/IncompleteModules", this, this, "reports");
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    columnModulePordnr.setCellValueFactory(new PropertyValueFactory<>("pordnr"));
    columnModuleTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
  }

  public void setData(final List<Module> incompleteModules) {
    tableViewModules.setItems(FXCollections.observableList(incompleteModules));
  }
}

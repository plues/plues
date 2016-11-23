package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Set;

public class RedundantUnitGroups extends VBox implements Initializable {

  @FXML
  @SuppressWarnings("unused")
  private TableView<Unit> tableViewRedundantUnitGroups;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Unit, Integer> columnUnitId;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Unit, String> columnUnitTitle;

  @Inject
  public RedundantUnitGroups(final Inflater inflater) {
    inflater.inflate("/components/reports/RedundantUnitGroups", this, this, "reports", "Column");
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    columnUnitId.setCellValueFactory(new PropertyValueFactory<>("key"));
    columnUnitTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
  }

  public void setData(Set<Unit> redundantUnitGroups) {
    tableViewRedundantUnitGroups.setItems(
        FXCollections.observableList(new ArrayList<>(redundantUnitGroups)));
  }
}

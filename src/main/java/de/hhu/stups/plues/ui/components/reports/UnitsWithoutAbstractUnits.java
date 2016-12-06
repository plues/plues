package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

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
import java.util.List;
import java.util.ResourceBundle;

public class UnitsWithoutAbstractUnits extends VBox implements Initializable {

  @FXML
  @SuppressWarnings("unused")
  private TableView<Unit> tableViewUnits;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Unit, String> tableColumnUnitKey;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Unit, String> tableColumnUnitTitle;

  @Inject
  public UnitsWithoutAbstractUnits(final Inflater inflater) {
    inflater.inflate("components/reports/UnitsWithoutAbstractUnits",
        this, this, "reports", "Column");
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    tableColumnUnitKey.setCellValueFactory(new PropertyValueFactory<>("key"));
    tableColumnUnitTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
  }

  public void setData(final List<Unit> unitsWithoutAbstractUnits) {
    tableViewUnits.setItems(FXCollections.observableList(unitsWithoutAbstractUnits));
  }
}

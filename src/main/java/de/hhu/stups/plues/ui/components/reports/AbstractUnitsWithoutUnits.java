package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AbstractUnitsWithoutUnits extends VBox implements Initializable {

  @FXML
  @SuppressWarnings("unused")
  private TableView<AbstractUnit> tableViewAbstractUnits;

  @Inject
  public AbstractUnitsWithoutUnits(final Inflater inflater) {
    inflater.inflate("components/reports/AbstractUnitsWithoutUnits",
        this, this, "reports", "Column");
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {}

  public void setData(final List<AbstractUnit> abstractUnitsWithoutUnits) {
    tableViewAbstractUnits.setItems(FXCollections.observableList(abstractUnitsWithoutUnits));
  }
}

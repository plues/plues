package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class UnitsWithoutAbstractUnits extends VBox implements Initializable {

  @FXML
  @SuppressWarnings("unused")
  private TableView<Unit> tableViewUnits;
  @FXML
  @SuppressWarnings("unused")
  private Text txtExplanation;

  @Inject
  public UnitsWithoutAbstractUnits(final Inflater inflater) {
    inflater.inflate("components/reports/UnitsWithoutAbstractUnits",
        this, this, "reports", "Column");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    txtExplanation.wrappingWidthProperty().bind(tableViewUnits.widthProperty().subtract(25.0));
  }

  public void setData(final List<Unit> unitsWithoutAbstractUnits) {
    tableViewUnits.setItems(FXCollections.observableList(unitsWithoutAbstractUnits));
  }
}

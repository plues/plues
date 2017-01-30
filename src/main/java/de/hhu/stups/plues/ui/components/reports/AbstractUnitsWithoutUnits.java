package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AbstractUnitsWithoutUnits extends VBox implements Initializable {

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

  @Inject
  public AbstractUnitsWithoutUnits(final Inflater inflater) {
    inflater.inflate("components/reports/AbstractUnitsWithoutUnits",
        this, this, "reports", "Column");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    txtExplanation.wrappingWidthProperty().bind(
        tableViewAbstractUnits.widthProperty().subtract(25.0));
    bindTableColumnsWidth();
  }

  private void bindTableColumnsWidth() {
    tableColumnAbstractUnitKey.prefWidthProperty().bind(
        tableViewAbstractUnits.widthProperty().multiply(0.2));
    tableColumnAbstractUnitTitle.prefWidthProperty().bind(
        tableViewAbstractUnits.widthProperty().multiply(0.76));
  }

  public void setData(final List<AbstractUnit> abstractUnitsWithoutUnits) {
    tableViewAbstractUnits.setItems(FXCollections.observableList(abstractUnitsWithoutUnits));
  }
}

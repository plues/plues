package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.ui.components.detailview.DetailViewHelper;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.List;

public class UnitsWithoutAbstractUnits extends VBox {

  private final Router router;
  @FXML
  @SuppressWarnings("unused")
  private TableView<Unit> tableViewUnits;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> tableColumnUnitKey;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> tableColumnUnitTitle;
  @FXML
  @SuppressWarnings("unused")
  private Text txtExplanation;

  /**
   * Default constructor.
   *
   * @param inflater Handle fxml and resources.
   * @param router   Router.
   */
  @Inject
  public UnitsWithoutAbstractUnits(final Inflater inflater, final Router router) {
    this.router = router;
    inflater.inflate("components/reports/UnitsWithoutAbstractUnits",
        this, this, "reports", "Column");
  }

  @FXML
  public void initialize() {
    tableViewUnits.setOnMouseClicked(DetailViewHelper.getUnitMouseHandler(tableViewUnits, router));
    txtExplanation.wrappingWidthProperty().bind(tableViewUnits.widthProperty().subtract(25.0));
  }

  public void setData(final List<Unit> unitsWithoutAbstractUnits) {
    tableViewUnits.setItems(FXCollections.observableList(unitsWithoutAbstractUnits));
  }
}

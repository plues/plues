package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.ui.components.detailview.DetailViewHelper;
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

public class UnitsWithoutAbstractUnits extends VBox implements Initializable {

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
   * @param router Router.
   */
  @Inject
  public UnitsWithoutAbstractUnits(final Inflater inflater, final Router router) {
    this.router = router;
    inflater.inflate("components/reports/UnitsWithoutAbstractUnits",
        this, this, "reports", "Column");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    tableViewUnits.setOnMouseClicked(DetailViewHelper.getUnitMouseHandler(tableViewUnits, router));
    txtExplanation.wrappingWidthProperty().bind(tableViewUnits.widthProperty().subtract(25.0));
    bindTableColumnsWidth();
  }

  private void bindTableColumnsWidth() {
    tableColumnUnitKey.prefWidthProperty().bind(tableViewUnits.widthProperty().multiply(0.2));
    tableColumnUnitTitle.prefWidthProperty().bind(tableViewUnits.widthProperty().multiply(0.76));
  }

  public void setData(final List<Unit> unitsWithoutAbstractUnits) {
    tableViewUnits.setItems(FXCollections.observableList(unitsWithoutAbstractUnits));
  }
}

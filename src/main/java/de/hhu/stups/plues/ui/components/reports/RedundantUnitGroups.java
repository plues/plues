package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

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

import java.util.ArrayList;
import java.util.Set;

public class RedundantUnitGroups extends VBox {

  private final Router router;

  @FXML
  @SuppressWarnings("unused")
  private TableView<Unit> tableViewRedundantUnitGroups;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Unit, String> tableColumnUnitKey;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Unit, String> tableColumnUnitTitle;
  @FXML
  @SuppressWarnings("unused")
  private Text txtExplanation;

  @Inject
  public RedundantUnitGroups(final Inflater inflater, final Router router) {
    this.router = router;
    inflater.inflate("components/reports/RedundantUnitGroups", this, this, "reports", "Column");
  }

  @FXML
  public void initialize() {
    txtExplanation.wrappingWidthProperty().bind(
        tableViewRedundantUnitGroups.widthProperty().subtract(25.0));
    
    tableViewRedundantUnitGroups.setOnMouseClicked(
        DetailViewHelper.getUnitMouseHandler(tableViewRedundantUnitGroups, router));
  }

  public void setData(final Set<Unit> redundantUnitGroups) {
    tableViewRedundantUnitGroups.setItems(
        FXCollections.observableList(new ArrayList<>(redundantUnitGroups)));
  }
}

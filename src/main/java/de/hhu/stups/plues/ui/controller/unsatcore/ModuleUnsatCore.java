package de.hhu.stups.plues.ui.controller.unsatcore;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.ui.components.detailview.DetailViewHelper;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ResourceBundle;

public class ModuleUnsatCore extends VBox implements Initializable {

  private final ListProperty<Module> modules;
  private final Router router;

  @FXML
  @SuppressWarnings("unused")
  private TableView<Module> modulesTable;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, Boolean> moduleTypeColumn;
  @FXML
  @SuppressWarnings("unused")
  private UnsatCoreButtonBar unsatCoreButtonBar;
  @FXML
  @SuppressWarnings("unused")
  private Text txtExplanation;

  /**
   * Default constructor.
   */
  @Inject
  public ModuleUnsatCore(final Inflater inflater, final Router router) {
    this.router = router;

    modules = new SimpleListProperty<>(FXCollections.emptyObservableList());

    inflater.inflate("components/unsatcore/ModuleUnsatCore", this, this, "unsatCore", "Column");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    txtExplanation.wrappingWidthProperty().bind(widthProperty().subtract(150));

    modulesTable.itemsProperty().bind(modules);
    modulesTable.setOnMouseClicked(DetailViewHelper.getModuleMouseHandler(
        modulesTable, router));
    moduleTypeColumn.setCellFactory(param -> new TableCell<Module, Boolean>() {
      @Override
      protected void updateItem(final Boolean item, final boolean empty) {
        super.updateItem(item, empty);
        if (item == null || empty) {
          setText(null);
          return;
        }
        setText(item ? "✔︎" : "✗");
      }
    });

    unsatCoreButtonBar.setText(resources.getString("button.unsatCoreAbstractUnits"));
  }

  void resetTaskState() {
    unsatCoreButtonBar.resetTaskState();
  }

  public void setModules(final ObservableList<Module> modules) {
    this.modules.set(modules);
  }

  public ObservableList<Module> getModules() {
    return modules.get();
  }

  ListProperty<Module> getModuleProperty() {
    return modules;
  }

  UnsatCoreButtonBar getUnsatCoreButtonBar() {
    return unsatCoreButtonBar;
  }
}

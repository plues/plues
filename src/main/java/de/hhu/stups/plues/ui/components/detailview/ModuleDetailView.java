package de.hhu.stups.plues.ui.components.detailview;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.ModuleLevel;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class ModuleDetailView extends VBox implements Initializable, DetailView {

  private final ObjectProperty<Module> moduleProperty;
  private final Router router;

  @FXML
  @SuppressWarnings("unused")
  private Label pordnr;
  @FXML
  @SuppressWarnings("unused")
  private Label title;
  @FXML
  @SuppressWarnings("unused")
  private Label name;
  @FXML
  @SuppressWarnings("unused")
  private Label mandatory;
  @FXML
  @SuppressWarnings("unused")
  private Label creditPoints;
  @FXML
  @SuppressWarnings("unused")
  private Label electiveUnits;
  @FXML
  @SuppressWarnings("unused")
  private TableView<ModuleLevel> moduleLevelTableView;
  @FXML
  @SuppressWarnings("unused")
  private TableView<AbstractUnit> abstractUnitTableView;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<ModuleLevel, String> tableColumnCourseName;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<ModuleLevel, String> tableColumnCourseColumnName;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<ModuleLevel, Boolean> tableColumnElectability;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<ModuleLevel, String>  tableColumnCreditPoints;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<AbstractUnit, String> tableColumnAbstractUnitKey;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<AbstractUnit, String> tableColumnAbstractUnitTitle;

  /**
   * Constructor for ModuleDetailView.
   *
   * @param inflater Inflater to handle fxml and lang files
   */
  @Inject
  public ModuleDetailView(final Inflater inflater,
                          final Router router) {
    moduleProperty = new SimpleObjectProperty<>();
    this.router = router;

    inflater.inflate("/components/detailview/ModuleDetailView", this, this, "detailView", "Column");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    bindField(pordnr.textProperty(), "pordnr");
    bindField(title.textProperty(), "title");
    bindField(electiveUnits.textProperty(), "electiveUnits");

    tableViewBindings();
  }

  private void tableViewBindings() {
    moduleLevelTableView.itemsProperty().bind(new ModuleLevelTableBinding());
    //
    tableColumnCourseName.setCellValueFactory(param
        -> Bindings.selectString(param.getValue(), "course", "name"));
    tableColumnCourseColumnName.setCellValueFactory(param
        -> Bindings.selectString(param.getValue(), "course", "fullName"));
    tableColumnElectability.setCellValueFactory(new PropertyValueFactory<>("mandatory"));
    tableColumnCreditPoints.setCellValueFactory(new PropertyValueFactory<>("creditPoints"));
    //
    tableColumnElectability.setCellFactory(param -> new TableCell<ModuleLevel, Boolean>() {
      @Override
      protected void updateItem(final Boolean item, final boolean empty) {
        super.updateItem(item, empty);
        if (item == null || empty) {
          setText("");
          return;
        }
        setText(item ? "✔︎" : "✗");
      }
    });
    //
    moduleLevelTableView.setOnMouseClicked(DetailViewHelper.getModuleLevelHandler(
        moduleLevelTableView, router));
    //
    abstractUnitTableView.itemsProperty().bind(new AbstractUnitTableBinding());
    abstractUnitTableView.setOnMouseClicked(DetailViewHelper.getAbstractUnitMouseHandler(
        abstractUnitTableView, router));
  }

  private void bindField(final StringProperty stringProperty, final String name) {
    stringProperty.bind(Bindings.when(moduleProperty.isNotNull())
        .then(Bindings.selectString(moduleProperty, name))
        .otherwise(""));
  }

  public void setModule(final Module module) {
    this.moduleProperty.set(module);
  }

  public String getTitle() {
    return moduleProperty.get().getTitle();
  }

  private class ModuleLevelTableBinding extends ListBinding<ModuleLevel> {
    ModuleLevelTableBinding() {
      bind(moduleProperty);
    }

    @Override
    protected ObservableList<ModuleLevel> computeValue() {
      final Module module = moduleProperty.get();
      if (module == null) {
        return FXCollections.emptyObservableList();
      }

      return FXCollections.observableArrayList(module.getModuleLevels());
    }
  }

  private class AbstractUnitTableBinding extends ListBinding<AbstractUnit> {
    AbstractUnitTableBinding() {
      bind(moduleProperty);
    }

    @Override
    protected ObservableList<AbstractUnit> computeValue() {
      final Module module = moduleProperty.get();
      if (module == null) {
        return FXCollections.emptyObservableList();
      }

      return FXCollections.observableArrayList(module.getAbstractUnits());
    }
  }
}

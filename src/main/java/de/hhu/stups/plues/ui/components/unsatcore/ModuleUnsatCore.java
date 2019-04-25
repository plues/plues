package de.hhu.stups.plues.ui.components.unsatcore;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.ModuleLevel;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.ui.components.detailview.DetailViewHelper;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ModuleUnsatCore extends VBox implements Initializable {

  private final ListProperty<Module> modules;
  private final Router router;

  @FXML
  @SuppressWarnings("unused")
  private TableView<Module> modulesTable;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> tableColumnModulePordnr;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> tableColumnModuleName;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Module, String> tableColumnModuleType;
  @FXML
  @SuppressWarnings("unused")
  private UnsatCoreButtonBar unsatCoreButtonBar;
  @FXML
  @SuppressWarnings("unused")
  private Text txtExplanation;
  @SuppressWarnings("unused")
  private ObservableList<Course> courses;

  /**
   * Default constructor.
   */
  @Inject
  public ModuleUnsatCore(final Inflater inflater, final Router router) {
    this.router = router;

    modules = new SimpleListProperty<>(FXCollections.emptyObservableList());
    courses = FXCollections.emptyObservableList();

    inflater.inflate("components/unsatcore/ModuleUnsatCore", this, this, "unsatCore", "Column");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    txtExplanation.wrappingWidthProperty().bind(widthProperty().subtract(150));

    modulesTable.itemsProperty().bind(modules);
    modulesTable.setOnMouseClicked(DetailViewHelper.getModuleMouseHandler(
        modulesTable, router));
    tableColumnModuleType.setCellValueFactory(param -> param.getValue().getModuleLevels() == null ?
      new SimpleStringProperty("") :
      param.getValue().getModuleLevels().stream()
          .filter(moduleLevel -> this.courses.contains(moduleLevel.getCourse()))
          .map(ModuleLevel::getMandatory)
          .distinct()
          .map(item -> item ? "✔︎" : "✗")
          .collect(
            Collectors.collectingAndThen(
            Collectors.joining(", "), ReadOnlyStringWrapper::new)));

    unsatCoreButtonBar.setSubmitText(resources.getString("button.unsatCoreAbstractUnits"));
  }

  public void resetTaskState() {
    unsatCoreButtonBar.taskProperty().set(null);
  }

  public void setModules(final ObservableList<Module> modules) {
    this.modules.set(modules);
  }

  public ObservableList<Module> getModules() {
    return modules.get();
  }

  public ListProperty<Module> moduleProperty() {
    return modules;
  }

  public UnsatCoreButtonBar getUnsatCoreButtonBar() {
    return unsatCoreButtonBar;
  }

  public void setCourses(final ObservableList<Course> courses) {
    this.courses = courses;
  }
}

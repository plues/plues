package de.hhu.stups.plues.ui.components.detailview;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class ModuleDetailView extends VBox implements Initializable {

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
  private TableView<Course> courseTableView;
  @FXML
  @SuppressWarnings("unused")
  private TableView<AbstractUnit> abstractUnitTableView;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Course, String> tableColumnCourseName;
  @FXML
  @SuppressWarnings("unused")
  private TableColumn<Course, String> tableColumnCourseColumnName;
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
    bindField(name.textProperty(), "name");
    bindField(creditPoints.textProperty(), "creditPoints");
    bindField(electiveUnits.textProperty(), "electiveUnits");

    mandatory.textProperty().bind(Bindings.when(moduleProperty.isNotNull())
        .then(Bindings.when(Bindings.selectBoolean(moduleProperty, "mandatory"))
            .then("✔︎")
            .otherwise("✗"))
        .otherwise("?"));

    bindTableColumnsWidth();

    tableViewBindings();
  }

  private void tableViewBindings() {
    courseTableView.itemsProperty().bind(new CourseTableBinding());
    courseTableView.setOnMouseClicked(DetailViewHelper.getCourseMouseHandler(
        courseTableView, router));

    abstractUnitTableView.itemsProperty().bind(new AbstractUnitTableBinding());
    abstractUnitTableView.setOnMouseClicked(DetailViewHelper.getAbstractUnitMouseHandler(
        abstractUnitTableView, router));
  }

  private void bindField(StringProperty stringProperty, String name) {
    stringProperty.bind(Bindings.when(moduleProperty.isNotNull())
        .then(Bindings.selectString(moduleProperty, name))
        .otherwise(""));
  }

  private void bindTableColumnsWidth() {
    tableColumnCourseName.prefWidthProperty().bind(
        courseTableView.widthProperty().multiply(0.25));
    tableColumnCourseColumnName.prefWidthProperty().bind(
        courseTableView.widthProperty().multiply(0.71));

    tableColumnAbstractUnitKey.prefWidthProperty().bind(
        abstractUnitTableView.widthProperty().multiply(0.25));
    tableColumnAbstractUnitTitle.prefWidthProperty().bind(
        abstractUnitTableView.widthProperty().multiply(0.71));

  }

  public void setModule(final Module module) {
    this.moduleProperty.set(module);
  }

  public String getTitle() {
    return moduleProperty.get().getTitle();
  }

  private class CourseTableBinding extends ListBinding<Course> {
    CourseTableBinding() {
      bind(moduleProperty);
    }

    @Override
    protected ObservableList<Course> computeValue() {
      final Module module = moduleProperty.get();
      if (module == null) {
        return FXCollections.emptyObservableList();
      }

      return FXCollections.observableArrayList(module.getCourses());
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

package de.hhu.stups.plues.ui.components.detailview;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ListBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class ModuleDetailView extends VBox implements Initializable {

  private final ObjectProperty<Module> moduleProperty;
  private final Provider<AbstractUnitDetailView> abstractUnitDetailViewProvider;

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

  /**
   * Constructor for ModuleDetailView.
   * @param inflater Inflater to handle fxml and lang files
   * @param abstractUnitDetailViewProvider Provider for abstractUnitDetailView to handle navigation
   */
  @Inject
  public ModuleDetailView(final Inflater inflater,
                          final Provider<AbstractUnitDetailView> abstractUnitDetailViewProvider) {
    moduleProperty = new SimpleObjectProperty<>();
    this.abstractUnitDetailViewProvider = abstractUnitDetailViewProvider;

    inflater.inflate("/components/detailview/ModuleDetailView", this, this, "detailView", "Column");
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    pordnr.textProperty().bind(Bindings.when(moduleProperty.isNotNull()).then(
        Bindings.selectString(moduleProperty, "pordnr")).otherwise(""));
    title.textProperty().bind(Bindings.when(moduleProperty.isNotNull()).then(
        Bindings.selectString(moduleProperty, "title")).otherwise(""));
    name.textProperty().bind(Bindings.when(moduleProperty.isNotNull()).then(
        Bindings.selectString(moduleProperty, "name")).otherwise(""));
    mandatory.textProperty().bind(Bindings.createStringBinding(() -> {
      Module module = moduleProperty.get();
      if (module == null) {
        return "?";
      }

      return module.getMandatory() ? "✔︎" : "✗";
    }, moduleProperty));
    creditPoints.textProperty().bind(Bindings.when(moduleProperty.isNotNull()).then(
        Bindings.selectString(moduleProperty, "creditPoints")).otherwise(""));
    electiveUnits.textProperty().bind(Bindings.when(moduleProperty.isNotNull()).then(
        Bindings.selectString(moduleProperty, "electiveUnits")).otherwise(""));

    courseTableView.itemsProperty().bind(new ListBinding<Course>() {
      {
        bind(moduleProperty);
      }

      @Override
      protected ObservableList<Course> computeValue() {
        Module module = moduleProperty.get();
        if (module == null) {
          return FXCollections.emptyObservableList();
        }

        return FXCollections.observableArrayList(module.getCourses());
      }
    });
    abstractUnitTableView.itemsProperty().bind(new ListBinding<AbstractUnit>() {
      {
        bind(moduleProperty);
      }

      @Override
      protected ObservableList<AbstractUnit> computeValue() {
        Module module = moduleProperty.get();
        if (module == null) {
          return FXCollections.emptyObservableList();
        }

        return FXCollections.observableArrayList(module.getAbstractUnits());
      }
    });

    abstractUnitTableView.setOnMouseClicked(DetailViewHelper.getAbstractUnitMouseHandler(
        abstractUnitTableView, abstractUnitDetailViewProvider));
  }

  public void setModule(Module module) {
    this.moduleProperty.set(module);
  }

  public String getTitle() {
    return moduleProperty.get().getTitle();
  }
}

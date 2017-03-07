package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class ImpossibleCourseModuleAbstractUnits extends VBox implements Initializable {

  private static final String PAIR_FORMAT = "%s, %s";

  @FXML
  @SuppressWarnings("unused")
  private TreeView<String> treeViewCourseModuleAbstractUnits;
  @FXML
  @SuppressWarnings("unused")
  private Text txtExplanation;

  @Inject
  public ImpossibleCourseModuleAbstractUnits(final Inflater inflater) {
    inflater.inflate("components/reports/ImpossibleCourseModuleAbstractUnits",
        this, this, "reports");
  }

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
    treeViewCourseModuleAbstractUnits.setRoot(new TreeItem<>());
    txtExplanation.wrappingWidthProperty().bind(
        treeViewCourseModuleAbstractUnits.widthProperty().subtract(25.0));
  }

  /**
   * Set data for this component.
   *
   * @param courseModuleAbstractUnit data
   */
  public void setData(final Map<Course, Map<Module, Set<AbstractUnit>>> courseModuleAbstractUnit) {
    treeViewCourseModuleAbstractUnits.getRoot().getChildren().setAll(
        courseModuleAbstractUnit.entrySet().stream().map(courseMapEntry -> {
          final TreeItem<String> courseItem
              = new TreeItem<>(getCourseString(courseMapEntry.getKey()));
          courseItem.getChildren().setAll(
              courseMapEntry.getValue().entrySet().stream().map(moduleSetEntry -> {
                final TreeItem<String> moduleItem
                    = new TreeItem<>(getModuleString(moduleSetEntry.getKey()));
                moduleItem.getChildren().setAll(
                    moduleSetEntry.getValue().stream().map(abstractUnit ->
                        new TreeItem<>(getAbstractUnitString(abstractUnit)))
                        .collect(Collectors.toSet()));
                return moduleItem;
              }).collect(Collectors.toSet()));
          return courseItem;
        }).collect(Collectors.toSet()));
  }

  private String getCourseString(final Course course) {
    return String.format(PAIR_FORMAT, course.getKey(), course.getFullName());
  }

  private String getModuleString(final Module module) {
    return String.format(PAIR_FORMAT, module.getPordnr(), module.getTitle());
  }

  private String getAbstractUnitString(final AbstractUnit abstractUnit) {
    return String.format(PAIR_FORMAT, abstractUnit.getKey(), abstractUnit.getTitle());
  }
}

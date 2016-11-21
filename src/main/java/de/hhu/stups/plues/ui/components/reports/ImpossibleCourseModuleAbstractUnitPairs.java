package de.hhu.stups.plues.ui.components.reports;

import com.google.common.base.Joiner;
import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.prob.report.Pair;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class ImpossibleCourseModuleAbstractUnitPairs extends VBox implements Initializable {

  @FXML
  @SuppressWarnings("unused")
  private TreeView<String> treeViewCourseModuleAbstractUnitPairs;

  @Inject
  public ImpossibleCourseModuleAbstractUnitPairs(final Inflater inflater) {
    inflater.inflate("/components/reports/ImpossibleCourseModuleAbstractUnitPairs",
        this, this, "reports");
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    treeViewCourseModuleAbstractUnitPairs.setRoot(new TreeItem<>());
  }

  /**
   * Set data for this component.
   * @param courseModuleAbstractUnitPairs data
   */
  public void setData(final Map<Course, Map<Module, Set<Pair<AbstractUnit>>>>
      courseModuleAbstractUnitPairs) {
    treeViewCourseModuleAbstractUnitPairs.getRoot().getChildren().setAll(
        courseModuleAbstractUnitPairs.entrySet().stream().map(courseMapEntry -> {
          TreeItem<String> courseItem = new TreeItem<>(getCourseString(courseMapEntry.getKey()));
          courseItem.getChildren().setAll(
              courseMapEntry.getValue().entrySet().stream().map(moduleSetEntry -> {
                TreeItem<String> moduleItem =
                    new TreeItem<>(getModuleString(moduleSetEntry.getKey()));
                moduleItem.getChildren().setAll(
                    moduleSetEntry.getValue().stream().map(pair ->
                    new TreeItem<>(getAbstractUnitString(pair.getFirst(), pair.getSecond())))
                        .collect(Collectors.toSet()));
                return moduleItem;
              }).collect(Collectors.toSet()));
          return courseItem;
        }).collect(Collectors.toSet()));
  }

  private String getCourseString(Course course) {
    return Joiner.on(", ").join(course.getKey(), course.getFullName());
  }

  private String getModuleString(Module module) {
    return Joiner.on(", ").join(module.getPordnr(), module.getTitle());
  }

  private String getAbstractUnitString(AbstractUnit abstractUnit1, AbstractUnit abstractUnit2) {
    return Joiner.on("\n").join(
      Joiner.on(", ").join(abstractUnit1.getKey(), abstractUnit1.getTitle()),
      Joiner.on(", ").join(abstractUnit2.getKey(), abstractUnit2.getTitle()));
  }
}

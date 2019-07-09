package de.hhu.stups.plues.ui.components.reports;

import com.google.inject.Inject;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.ui.layout.Inflater;
import javafx.fxml.FXML;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ImpossibleCourseModuleAbstractUnitPairs extends VBox {

  private static final String PAIR_FORMAT = "%s, %s";

  @FXML
  @SuppressWarnings("unused")
  private TreeView<String> treeViewCourseModuleAbstractUnitPairs;
  @FXML
  @SuppressWarnings("unused")
  private Text txtExplanation;

  @Inject
  public ImpossibleCourseModuleAbstractUnitPairs(final Inflater inflater) {
    inflater.inflate("components/reports/ImpossibleCourseModuleAbstractUnitPairs",
        this, this, "reports");
  }

  @FXML
  public void initialize() {
    treeViewCourseModuleAbstractUnitPairs.setRoot(new TreeItem<>());
    txtExplanation.wrappingWidthProperty().bind(
        treeViewCourseModuleAbstractUnitPairs.widthProperty().subtract(25.0));
  }

  /**
   * Set data for this component.
   * @param courseModuleAbstractUnitPairs data
   */
  public void setData(final Map<Course, Map<Module, Set<AbstractUnitPair>>>
      courseModuleAbstractUnitPairs) {
    treeViewCourseModuleAbstractUnitPairs.getRoot().getChildren().setAll(
        courseModuleAbstractUnitPairs.entrySet().stream().map(courseMapEntry -> {
          final TreeItem<String> courseItem
              = new TreeItem<>(getCourseString(courseMapEntry.getKey()));
          courseItem.getChildren().setAll(
              courseMapEntry.getValue().entrySet().stream().map(moduleSetEntry -> {
                final TreeItem<String> moduleItem
                    = new TreeItem<>(getModuleString(moduleSetEntry.getKey()));
                moduleItem.getChildren().setAll(
                    moduleSetEntry.getValue().stream().map(pair ->
                    new TreeItem<>(getAbstractUnitString(pair.getFirst(), pair.getSecond())))
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

  private String getAbstractUnitString(final AbstractUnit abstractUnit1,
      final AbstractUnit abstractUnit2) {
    return String.format("%s, %s%n%s, %s", abstractUnit1.getKey(), abstractUnit1.getTitle(),
                                           abstractUnit2.getKey(), abstractUnit2.getTitle());
  }
}

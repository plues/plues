package de.hhu.stups.plues.ui.components.conflictmatrix;

import de.hhu.stups.plues.data.entities.Course;

import javafx.beans.property.ObjectProperty;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

import java.util.ResourceBundle;

class CourseContextMenu extends ContextMenu {

  CourseContextMenu(final Course course,
                    final ObjectProperty<Course> checkAllCombinationsCourseProperty) {
    final ResourceBundle resources = ResourceBundle.getBundle("lang.conflictMatrixContextMenu");
    final MenuItem itemCheckAllCombinations =
        new MenuItem(resources.getString("checkAllCombinations"));
    itemCheckAllCombinations.setOnAction(event -> checkAllCombinationsCourseProperty.set(course));
    getItems().add(itemCheckAllCombinations);
  }
}

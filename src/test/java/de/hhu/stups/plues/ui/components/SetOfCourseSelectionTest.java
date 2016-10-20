package de.hhu.stups.plues.ui.components;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.testfx.framework.junit.ApplicationTest;

import java.util.ArrayList;
import java.util.List;

@RunWith(JUnit4.class)
public class SetOfCourseSelectionTest extends ApplicationTest {
  private SetOfCourseSelection courseSelection;
  private List<Course> courseList;

  @Test
  public void selectionTest() {
    TableView<SetOfCourseSelection.TableRowPair<Node, String>> tableViewMasterCourse;
    TableView<SetOfCourseSelection.TableRowPair<Node, String>> tableViewBachelorCourse;

    tableViewMasterCourse = courseSelection.getTableViewMasterCourse();
    tableViewBachelorCourse = courseSelection.getTableViewBachelorCourse();

    clickOn(tableViewBachelorCourse.getItems().get(0).getFirst());
    clickOn(tableViewBachelorCourse.getItems().get(1).getFirst());

    clickOn(tableViewMasterCourse.getItems().get(0).getFirst());

    Assert.assertTrue(courseSelection.getSelectedCourses().equals(
        FXCollections.observableArrayList(courseList.get(0),courseList.get(1),courseList.get(3))));

  }

  private Course createCourse(final String shortName, final String degree) {
    final Course course = new Course();
    course.setShortName(shortName);
    course.setLongName(shortName);
    course.setDegree(degree);
    return course;
  }

  @Override
  public void start(final Stage stage) throws Exception {

    courseList = new ArrayList<>();
    courseList.add(createCourse("shortName1", "bk"));
    courseList.add(createCourse("shortName2", "bk"));
    courseList.add(createCourse("shortName3", "ba"));
    courseList.add(createCourse("shortName4", ""));
    courseList.add(createCourse("shortName5", ""));

    Inflater inflater = new Inflater(new FXMLLoader());
    courseSelection = new SetOfCourseSelection(inflater);

    courseSelection.setCourses(courseList);

    final Scene scene = new Scene(courseSelection, 400, 700);

    stage.setScene(scene);
    stage.show();
  }
}

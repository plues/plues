package de.hhu.stups.plues.ui.components;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import org.junit.Assert;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import java.util.ArrayList;
import java.util.List;

public class SetOfCourseSelectionTest extends ApplicationTest {
  private SetOfCourseSelection courseSelection;
  private List<Course> courseList;

  @Test
  public void selectionTest() {
    final TableView<?> tableViewMasterCourse;
    final TableView<?> tableViewBachelorCourse;

    tableViewMasterCourse = courseSelection.getTableViewMasterCourse();
    tableViewBachelorCourse = courseSelection.getTableViewBachelorCourse();
    final List<Node> masterCheckBoxes
        = new ArrayList<>(tableViewMasterCourse.lookupAll(".check-box"));
    final List<Node> bachelorCheckBoxes
        = new ArrayList<>(tableViewBachelorCourse.lookupAll(".check-box"));

    Assert.assertTrue(courseSelection.getSelectedCourses().isEmpty());


    clickOn(bachelorCheckBoxes.get(0));
    clickOn(bachelorCheckBoxes.get(1));

    clickOn(masterCheckBoxes.get(0));

    Assert.assertEquals(3, courseSelection.getSelectedCourses().size());
    Assert.assertTrue(courseSelection.getSelectedCourses().contains(courseList.get(0)));
    Assert.assertTrue(courseSelection.getSelectedCourses().contains(courseList.get(1)));
    Assert.assertTrue(courseSelection.getSelectedCourses().contains(courseList.get(3)));

    clickOn(bachelorCheckBoxes.get(1));

    Assert.assertEquals(2, courseSelection.getSelectedCourses().size());
    Assert.assertTrue(courseSelection.getSelectedCourses().contains(courseList.get(0)));
    Assert.assertTrue(courseSelection.getSelectedCourses().contains(courseList.get(3)));

    clickOn(bachelorCheckBoxes.get(0));
    clickOn(masterCheckBoxes.get(0));
    Assert.assertTrue(courseSelection.getSelectedCourses().isEmpty());

    masterCheckBoxes.forEach(item -> clickOn(item));
    bachelorCheckBoxes.forEach(item -> clickOn(item));
    Assert.assertTrue(courseSelection.getSelectedCourses().equals(courseList));
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
    courseList.add(createCourse("shortName4", "ma"));
    courseList.add(createCourse("shortName5", "ma"));

    final Inflater inflater = new Inflater(new FXMLLoader());
    courseSelection = new SetOfCourseSelection(inflater);

    courseSelection.setCourses(courseList);

    final Scene scene = new Scene(courseSelection, 400, 700);

    stage.setScene(scene);
    stage.show();
  }
}

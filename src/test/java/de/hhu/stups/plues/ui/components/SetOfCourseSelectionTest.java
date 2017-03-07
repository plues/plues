package de.hhu.stups.plues.ui.components;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import org.junit.Assert;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import java.util.ArrayList;
import java.util.List;

public class SetOfCourseSelectionTest extends ApplicationTest {
  private SetOfCourseSelection courseSelection;
  private List<Course> courseList;

  private List<Node> masterCheckBoxes;
  private List<Node> bachelorCheckBoxes;

  @Test
  public void selectionTest() {
    masterCheckBoxes = new ArrayList<>(
        courseSelection.getTableViewMasterCourse().lookupAll(".check-box"));
    bachelorCheckBoxes = new ArrayList<>(
        courseSelection.getTableViewBachelorCourse().lookupAll(".check-box"));

    Assert.assertTrue(courseSelection.getSelectedCourses().isEmpty());

    clickOn(bachelorCheckBoxes.get(0));
    clickOn(bachelorCheckBoxes.get(1));

    clickOn(masterCheckBoxes.get(0));

    Assert.assertEquals(3, courseSelection.getSelectedCourses().size());
    Assert.assertTrue(courseSelection.getSelectedCourses().contains(courseList.get(0)));
    Assert.assertTrue(courseSelection.getSelectedCourses().contains(courseList.get(1)));
    Assert.assertTrue(courseSelection.getSelectedCourses().contains(courseList.get(4)));

    clickOn(bachelorCheckBoxes.get(1));

    Assert.assertEquals(2, courseSelection.getSelectedCourses().size());
    Assert.assertTrue(courseSelection.getSelectedCourses().contains(courseList.get(0)));
    Assert.assertTrue(courseSelection.getSelectedCourses().contains(courseList.get(4)));

    clickOn(bachelorCheckBoxes.get(0));
    clickOn(masterCheckBoxes.get(0));
    Assert.assertTrue(courseSelection.getSelectedCourses().isEmpty());

    masterCheckBoxes.forEach(item -> clickOn(item));
    bachelorCheckBoxes.forEach(item -> clickOn(item));
    Assert.assertTrue(courseSelection.getSelectedCourses().equals(courseList));
  }

  @Test
  public void testClearSelection() {
    masterCheckBoxes = new ArrayList<>(
        courseSelection.getTableViewMasterCourse().lookupAll(".check-box"));
    bachelorCheckBoxes = new ArrayList<>(
        courseSelection.getTableViewBachelorCourse().lookupAll(".check-box"));

    final Button btClearSelection = lookup("#btClearSelection").query();
    clickOn(bachelorCheckBoxes.get(0));
    clickOn(bachelorCheckBoxes.get(1));
    clickOn(masterCheckBoxes.get(0));
    Assert.assertFalse(courseSelection.getSelectedCourses().isEmpty());
    clickOn(btClearSelection);
    Assert.assertTrue(courseSelection.getSelectedCourses().isEmpty());

    clickOn(bachelorCheckBoxes.get(3));
    Assert.assertFalse(courseSelection.getSelectedCourses().isEmpty());
    clickOn(btClearSelection);
    Assert.assertTrue(courseSelection.getSelectedCourses().isEmpty());

    clickOn(btClearSelection);
    Assert.assertTrue(courseSelection.getSelectedCourses().isEmpty());
  }

  private Course createCourse(final String shortName, final String degree) {
    final Course course = new Course();
    course.setShortName(shortName);
    course.setLongName(shortName);
    course.setDegree(degree);
    course.setCreditPoints(5);
    course.setPo(2016);
    return course;
  }

  @Override
  public void start(final Stage stage) throws Exception {
    courseList = new ArrayList<>();
    courseList.add(createCourse("shortName1", "bk"));
    courseList.add(createCourse("shortName2", "bk"));
    courseList.add(createCourse("shortName3", "bk"));
    courseList.add(createCourse("shortName4", "ba"));
    courseList.add(createCourse("shortName5", "ma"));
    courseList.add(createCourse("shortName6", "ma"));

    final Inflater inflater = new Inflater(new FXMLLoader());
    courseSelection = new SetOfCourseSelection(inflater);

    courseSelection.setCourses(courseList);

    final Scene scene = new Scene(courseSelection, 400, 700);

    stage.setScene(scene);
    stage.show();
  }
}

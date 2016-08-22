package de.hhu.stups.plues.ui.components;

import de.hhu.stups.plues.data.entities.Course;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.testfx.framework.junit.ApplicationTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

@RunWith(JUnit4.class)
public class MajorMinorCourseSelectionTest extends ApplicationTest {
  private MajorMinorCourseSelection courseSelection;
  private List<Course> majorCourseList;
  private List<Course> minorCourseList;

  @Test
  public void initialisationTest() {

    Assert.assertTrue(courseSelection.getMinorComboBox().isDisabled());
    Assert.assertEquals(majorCourseList.get(0), courseSelection.getSelectedMajorCourse());
    Assert.assertEquals(Optional.empty(), courseSelection.getSelectedMinorCourse());
    Assert.assertEquals(4, courseSelection.getMajorComboBox().getItems().size());
    Assert.assertEquals(4, courseSelection.getMinorComboBox().getItems().size());

    Assert.assertEquals(FXCollections.observableList(majorCourseList), courseSelection.getMajorComboBox().getItems());
    Assert.assertEquals(FXCollections.observableList(minorCourseList), courseSelection.getMinorComboBox().getItems());

  }

  @Test
  public void selectionTest() {

    // select combinable course
    clickOn(courseSelection.getMajorComboBox()).type(KeyCode.DOWN).type(KeyCode.ENTER);
    Assert.assertFalse(courseSelection.getMinorComboBox().isDisabled());
    Assert.assertEquals(majorCourseList.get(1), courseSelection.getSelectedMajorCourse());
    Assert.assertEquals(minorCourseList.get(0), courseSelection.getSelectedMinorCourse().get());
    Assert.assertEquals(4, courseSelection.getMajorComboBox().getItems().size());
    Assert.assertEquals(3, courseSelection.getMinorComboBox().getItems().size());

    // select not combinable course
    clickOn(courseSelection.getMajorComboBox()).
        type(KeyCode.DOWN).type(KeyCode.DOWN).type(KeyCode.ENTER);
    Assert.assertTrue(courseSelection.getMinorComboBox().isDisabled());
    Assert.assertEquals(majorCourseList.get(3), courseSelection.getSelectedMajorCourse());
    Assert.assertEquals(Optional.empty(), courseSelection.getSelectedMinorCourse());
    Assert.assertEquals(4, courseSelection.getMajorComboBox().getItems().size());
    Assert.assertEquals(3, courseSelection.getMinorComboBox().getItems().size());

  }

  // degree: "bk" is combinable, "ba" is not
  private Course createCourse(String shortName, String degree) {
    Course course = new Course();
    course.setShortName(shortName);
    course.setDegree(degree);
    return course;
  }

  @Override
  public void start(final Stage stage) throws Exception {

    majorCourseList = new ArrayList<>();
    majorCourseList.add(createCourse("shortName1", "ba"));
    majorCourseList.add(createCourse("shortName2", "bk"));
    majorCourseList.add(createCourse("shortName3", "bk"));
    majorCourseList.add(createCourse("shortName4", "ba"));

    minorCourseList = majorCourseList;

    courseSelection = new MajorMinorCourseSelection(new FXMLLoader());

    courseSelection.setMajorCourseList(FXCollections.observableList(majorCourseList));
    courseSelection.setMinorCourseList(FXCollections.observableList(minorCourseList));
    courseSelection.setInitialMinorCourseList(FXCollections.observableList(minorCourseList));

    final Scene scene = new Scene(this.courseSelection, 100, 100);

    stage.setScene(scene);
    stage.show();
  }
}

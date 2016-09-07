package de.hhu.stups.plues.ui.components;

import static javafx.collections.FXCollections.observableList;

import de.hhu.stups.plues.data.entities.Course;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.testfx.framework.junit.ApplicationTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RunWith(JUnit4.class)
public class MajorMinorCourseSelectionTest extends ApplicationTest {
  private MajorMinorCourseSelection courseSelection;
  private List<Course> majorCourseList;
  private List<Course> minorCourseList;

  @Test
  public void initialisationTest() {

    Assert.assertFalse(courseSelection.getMinorComboBox().isDisabled());
    Assert.assertEquals(majorCourseList.get(0), courseSelection.getSelectedMajorCourse());
    Assert.assertEquals(4, courseSelection.getMajorComboBox().getItems().size());

    Assert.assertTrue(courseSelection.getSelectedMajorCourse().isCombinable());
    Assert.assertEquals(2, courseSelection.getMinorComboBox().getItems().size());

    Assert.assertEquals(
        observableList(majorCourseList),
        courseSelection.getMajorComboBox().getItems());
    Assert.assertEquals(
        observableList(minorCourseList).filtered(course -> course.isCombinableWith(majorCourseList.get(0))),
        courseSelection.getMinorComboBox().getItems());
  }

  @Test
  public void selectionTest() {

    // select combinable course
    clickOn(courseSelection.getMajorComboBox()).type(KeyCode.DOWN).type(KeyCode.ENTER);
    Assert.assertFalse(courseSelection.getMinorComboBox().isDisabled());
    Assert.assertEquals(majorCourseList.get(1), courseSelection.getSelectedMajorCourse());
    Assert.assertEquals(minorCourseList.get(0), courseSelection.getSelectedMinorCourse().get());
    Assert.assertEquals(4, courseSelection.getMajorComboBox().getItems().size());
    Assert.assertEquals(2, courseSelection.getMinorComboBox().getItems().size());

    // select not combinable course
    clickOn(courseSelection.getMajorComboBox())
       .type(KeyCode.DOWN).type(KeyCode.DOWN).type(KeyCode.ENTER);
    Assert.assertTrue(courseSelection.getMinorComboBox().isDisabled());
    Assert.assertEquals(majorCourseList.get(3), courseSelection.getSelectedMajorCourse());
    Assert.assertEquals(Optional.empty(), courseSelection.getSelectedMinorCourse());
    Assert.assertEquals(4, courseSelection.getMajorComboBox().getItems().size());
    Assert.assertEquals(0, courseSelection.getMinorComboBox().getItems().size());

  }

  // degree: "bk" is combinable, "ba" is not
  private Course createCourse(final String shortName, final String degree) {
    final Course course = new Course();
    course.setShortName(shortName);
    course.setLongName(shortName);
    course.setDegree(degree);
    return course;
  }

  @Override
  public void start(final Stage stage) throws Exception {

    majorCourseList = new ArrayList<>();
    majorCourseList.add(createCourse("shortName1", "bk"));
    majorCourseList.add(createCourse("shortName2", "bk"));
    majorCourseList.add(createCourse("shortName3", "bk"));
    majorCourseList.add(createCourse("shortName4", "ba"));

    minorCourseList = majorCourseList;

    courseSelection = new MajorMinorCourseSelection(new FXMLLoader());

    courseSelection.setMinorCourseList(observableList(minorCourseList));
    courseSelection.setMajorCourseList(observableList(majorCourseList));

    final Scene scene = new Scene(this.courseSelection, 100, 100);

    stage.setScene(scene);
    stage.show();
  }
}

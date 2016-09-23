package de.hhu.stups.plues.ui.components;

import static org.testfx.matcher.base.NodeMatchers.hasText;

import de.hhu.stups.plues.data.entities.Course;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.matcher.control.TableViewMatchers;

import java.util.ArrayList;

@RunWith(JUnit4.class)
public class CourseFilterTest extends ApplicationTest {

  private static final String COURSE_LIST_VIEW = "#courseListView";
  private static final String SOME_SHORT_NAME = "some short name";
  private static final String LETTER = "LETTER";
  private CourseFilter courseFilter;
  private Course course;

  @Test
  public void selectedItemProperty() throws Exception {

    Assert.assertNull(this.courseFilter.selectedItem());
    Assert.assertNull(this.courseFilter.selectedItemProperty().get());

    final Node b = lookup(TableViewMatchers.hasTableCell(2016)).query();
    clickOn(b);

    Assert.assertEquals(this.courseFilter.selectedItem(), this.course);
    Assert.assertEquals(this.courseFilter.selectedItemProperty().get(), this.course);
  }

  @Test
  public void courseListDisplaysCourses() {

    FxAssert.verifyThat(COURSE_LIST_VIEW, TableViewMatchers.hasItems(1));

    FxAssert.verifyThat(COURSE_LIST_VIEW,
        TableViewMatchers.containsRow(0, SOME_SHORT_NAME, 2016, LETTER));

    FxAssert.verifyThat(COURSE_LIST_VIEW, TableViewMatchers.hasTableCell(2016));
    FxAssert.verifyThat(COURSE_LIST_VIEW, TableViewMatchers.hasTableCell(SOME_SHORT_NAME));
    FxAssert.verifyThat(COURSE_LIST_VIEW, TableViewMatchers.hasTableCell(LETTER));
  }

  @Test
  public void courseLabel() {
    FxAssert.verifyThat(".label", hasText("Courses"));
  }

  @Override
  public void start(final Stage stage) throws Exception {
    this.course = new Course();

    this.course.setShortName(SOME_SHORT_NAME);
    this.course.setPo(2016);
    this.course.setKzfa(LETTER);

    final ArrayList<Course> courses = new ArrayList<>();
    courses.add(this.course);

    this.courseFilter = new CourseFilter(new FXMLLoader());
    this.courseFilter.setCourses(courses);
    final Scene scene = new Scene(this.courseFilter, 100, 100);
    stage.setScene(scene);
    stage.show();
  }
}

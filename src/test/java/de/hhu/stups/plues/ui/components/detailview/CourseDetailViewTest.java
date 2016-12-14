package de.hhu.stups.plues.ui.components.detailview;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.ui.layout.Inflater;
import de.hhu.stups.plues.ui.layout.SceneFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.internal.stubbing.answers.ThrowsException;
import org.testfx.framework.junit.ApplicationTest;

public class CourseDetailViewTest extends ApplicationTest {

  private static final String KEY = "BK-PHI-H-2013";
  private static final String FULL_NAME = "Kernfach Philosophie (bk H) PO:2013";
  private static final String KZFA = "H";
  private static final String DEGREE = "bk";
  private static final int PO = 2013;
  private final Course course;

  /**
   * Test constructor.
   */
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
  public CourseDetailViewTest() {
    course = mock(Course.class, new ThrowsException(new RuntimeException()));

    doReturn(KEY).when(course).getKey();
    doReturn(FULL_NAME).when(course).getFullName();
    doReturn(KZFA).when(course).getKzfa();
    doReturn(DEGREE).when(course).getDegree();
    doReturn(PO).when(course).getPo();
  }

  @Test
  public void testCourseInfo() {
    final Label keyLabel = lookup("#key").query();
    Assert.assertEquals(KEY, keyLabel.getText());

    final Label nameLabel = lookup("#name").query();
    Assert.assertEquals(FULL_NAME, nameLabel.getText());

    final Label poLabel = lookup("#po").query();
    Assert.assertEquals(PO, Integer.parseInt(poLabel.getText()));

    final Label kzfaLabel = lookup("#kzfa").query();
    Assert.assertEquals(KZFA, kzfaLabel.getText());

    final Label degreeLabel = lookup("#degree").query();
    Assert.assertEquals(DEGREE, degreeLabel.getText());
  }

  @Override
  public void start(final Stage stage) throws Exception {
    final Inflater inflater = new Inflater(new FXMLLoader());

    final CourseDetailView courseDetailView = new CourseDetailView(inflater);
    courseDetailView.setCourse(course);

    stage.setScene(SceneFactory.create(courseDetailView));
    stage.show();
  }
}

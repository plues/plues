package de.hhu.stups.plues.ui.components.detailview;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.ui.layout.Inflater;
import de.hhu.stups.plues.ui.layout.SceneFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.internal.stubbing.answers.ThrowsException;
import org.testfx.framework.junit.ApplicationTest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class ModuleDetailViewTest extends ApplicationTest {

  private static final String TITLE = "Module 1";
  private static final int PORDNR = 1;
  private static final String NAME = "Mod";
  private static final String MANDATORY = "✗";
  private static final int CREDIT_POINTS = 100;
  private static final int ELECTIVE_UNITS = 0;
  private final List<Course> courses;
  private final List<AbstractUnit> abstractUnits;
  private Module module;

  /**
   * Test constructor.
   */
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
  public ModuleDetailViewTest() {
    final AbstractUnit au1 = mock(AbstractUnit.class, new ThrowsException(new RuntimeException()));
    final AbstractUnit au2 = mock(AbstractUnit.class, new ThrowsException(new RuntimeException()));
    module = mock(Module.class, new ThrowsException(new RuntimeException()));
    final Course course1 = mock(Course.class, new ThrowsException(new RuntimeException()));
    final Course course2 = mock(Course.class, new ThrowsException(new RuntimeException()));

    this.courses = Arrays.asList(course1, course2);
    this.abstractUnits = Arrays.asList(au1, au2);

    doReturn("Course 1").when(course1).getName();
    doReturn("BK-Course-1").when(course1).getFullName();
    doReturn("Course 2").when(course2).getName();
    doReturn("BK-Course-2").when(course2).getFullName();

    doReturn("AU-1").when(au1).getKey();
    doReturn("AU 1").when(au1).getTitle();
    doReturn("AU-2").when(au2).getKey();
    doReturn("AU 2").when(au2).getTitle();

    doReturn(new HashSet<>(courses)).when(module).getCourses();
    doReturn(new HashSet<>(abstractUnits)).when(module).getAbstractUnits();

    doReturn(TITLE).when(module).getTitle();
    doReturn(PORDNR).when(module).getPordnr();
    doReturn(NAME).when(module).getName();
    doReturn(false).when(module).getMandatory();
    doReturn(CREDIT_POINTS).when(module).getCreditPoints();
    doReturn(ELECTIVE_UNITS).when(module).getElectiveUnits();
  }

  @Test
  public void testModuleInfo() {
    final Label pordnrLabel = lookup("#pordnr").query();
    Assert.assertEquals(PORDNR, Integer.parseInt(pordnrLabel.getText()));

    final Label titleLabel = lookup("#title").query();
    Assert.assertEquals(TITLE, titleLabel.getText());

    final Label nameLabel = lookup("#name").query();
    Assert.assertEquals(NAME, nameLabel.getText());

    final Label mandatoryLabel = lookup("#mandatory").query();
    Assert.assertEquals(MANDATORY, mandatoryLabel.getText());

    final Label creditPointsLabel = lookup("#creditPoints").query();
    Assert.assertEquals(CREDIT_POINTS, Integer.parseInt(creditPointsLabel.getText()));

    final Label electiveUnitsLabel = lookup("#electiveUnits").query();
    Assert.assertEquals(ELECTIVE_UNITS, Integer.parseInt(electiveUnitsLabel.getText()));
  }

  @Test
  public void testContentSize() {
    final TableView<Course> courseTable = lookup("#courseTableView").query();
    final TableView<AbstractUnit> abstractUnitTableView = lookup("#abstractUnitTableView").query();

    Assert.assertEquals(courses.size(), courseTable.getItems().size());
    Assert.assertEquals(abstractUnits.size(), abstractUnitTableView.getItems().size());
  }

  @Test
  public void testCourseTableContent() {
    final TableView<Course> courseTable = lookup("#courseTableView").query();
    courses.forEach(course -> Assert.assertThat(courseTable.getItems(), hasItem(course)));
  }

  @Test
  public void testAbstractUnitTableContent() {
    final TableView<AbstractUnit> abstractUnitTableView = lookup("#abstractUnitTableView").query();
    abstractUnits.forEach(abstractUnit ->
        assertThat(abstractUnitTableView.getItems(), hasItem(abstractUnit)));
  }

  @Override
  public void start(final Stage stage) throws Exception {
    final Inflater inflater = new Inflater(new FXMLLoader());
    final Router router = new Router();

    final ModuleDetailView moduleDetailView = new ModuleDetailView(inflater, router);
    moduleDetailView.setModule(module);

    stage.setScene(SceneFactory.create(moduleDetailView));
    stage.show();
  }
}

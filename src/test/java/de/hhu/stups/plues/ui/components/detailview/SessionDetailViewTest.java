package de.hhu.stups.plues.ui.components.detailview;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Group;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.ModuleAbstractUnitSemester;
import de.hhu.stups.plues.data.entities.ModuleAbstractUnitType;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.routes.Router;
import de.hhu.stups.plues.ui.components.detailview.SessionDetailView.CourseTableEntry;
import de.hhu.stups.plues.ui.components.timetable.SessionFacade;
import de.hhu.stups.plues.ui.layout.Inflater;
import de.hhu.stups.plues.ui.layout.SceneFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.internal.stubbing.answers.ThrowsException;
import org.testfx.framework.junit.ApplicationTest;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
public class SessionDetailViewTest extends ApplicationTest {

  private final Store store;
  private final Map<Course, List<ModuleAbstractUnitSemester>> courseMap;
  private final List<Course> courses;
  private final List<Module> modules;
  private final List<AbstractUnit> abstractUnits;
  private final SessionFacade sessionFacade;

  /**
   * Test constructor.
   */
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public SessionDetailViewTest() {
    store = mock(Store.class);

    final ObjectProperty<SessionFacade.Slot> slot = new SimpleObjectProperty<>(
        new SessionFacade.Slot(DayOfWeek.MONDAY, 7));
    final Group group = mock(Group.class, new ThrowsException(new RuntimeException()));
    final Session session = mock(Session.class, new ThrowsException(new RuntimeException()));
    sessionFacade = mock(SessionFacade.class, new ThrowsException(new RuntimeException()));

    final AbstractUnit au1 = mock(AbstractUnit.class, new ThrowsException(new RuntimeException()));
    final AbstractUnit au2 = mock(AbstractUnit.class, new ThrowsException(new RuntimeException()));
    final ModuleAbstractUnitType maut1
        = mock(ModuleAbstractUnitType.class, new ThrowsException(new RuntimeException()));
    final ModuleAbstractUnitType maut2
        = mock(ModuleAbstractUnitType.class, new ThrowsException(new RuntimeException()));
    final Module mod1 = mock(Module.class, new ThrowsException(new RuntimeException()));
    final Module mod2 = mock(Module.class, new ThrowsException(new RuntimeException()));
    final Course course1 = mock(Course.class, new ThrowsException(new RuntimeException()));
    final Course course2 = mock(Course.class, new ThrowsException(new RuntimeException()));

    this.courses = Arrays.asList(course1, course2);
    this.modules = Arrays.asList(mod1, mod2);
    this.abstractUnits = Arrays.asList(au1, au2);

    doReturn("Course 1").when(course1).getKey();
    doReturn("Course 2").when(course2).getKey();

    Arrays.asList(mod1, mod2).forEach(module -> {
      doReturn(new HashSet<>(Arrays.asList(course1, course2))).when(module).getCourses();
      doReturn(new HashSet<>(Arrays.asList(1, 2))).when(module).getSemestersForAbstractUnit(au1);
      doReturn(new HashSet<>(Arrays.asList(3, 4))).when(module).getSemestersForAbstractUnit(au2);
    });

    doReturn("Module 1").when(mod1).getTitle();
    doReturn(1).when(mod1).getId();
    doReturn("Module 2").when(mod2).getTitle();
    doReturn(2).when(mod2).getId();

    doReturn(mod1).when(maut1).getModule();
    doReturn('m').when(maut1).getType();

    doReturn(mod2).when(maut2).getModule();
    doReturn('e').when(maut2).getType();

    doReturn(group).when(sessionFacade).getGroup();
    doReturn(1025).when(group).getId();

    doReturn(1).when(au1).getId();
    doReturn("AU-1").when(au1).getKey();
    doReturn("AU 1").when(au1).getTitle();
    doReturn(2).when(au2).getId();
    doReturn("AU-2").when(au2).getKey();
    doReturn("AU 2").when(au2).getTitle();

    doReturn(new HashSet<>(Arrays.asList(maut1, maut2))).when(au1).getModuleAbstractUnitTypes();
    doReturn(new HashSet<>(Arrays.asList(maut1, maut2))).when(au2).getModuleAbstractUnitTypes();

    courseMap = new HashMap<>();
    store.getCourses().forEach(course ->
        courseMap.put(course, store.getModuleAbstractUnitSemester()));

    doReturn(false).when(sessionFacade).isTentative();
    doReturn(session).when(sessionFacade).getSession();
    doReturn(slot).when(sessionFacade).slotProperty();

    doReturn("Unit").when(sessionFacade).getTitle();
    doReturn(slot.get()).when(sessionFacade).getSlot();
    doReturn(new HashSet<>(Arrays.asList(au1, au2)))
        .when(sessionFacade).getIntendedAbstractUnits();
    doReturn(new HashSet<>(Arrays.asList(1, 2)))
        .when(sessionFacade).getUnitSemesters();
  }

  @Test
  public void testContentSize() {
    final TableView courseTable = lookup("#courseTable").query();
    Assert.assertEquals(8, courseTable.getItems().size());
  }

  @Test
  public void testSessionInfo() {
    final Label sessionLabel = lookup("#lbSession").query();
    Assert.assertTrue(Arrays.asList("Montag, 20:30", "Monday, 20:30")
        .contains(sessionLabel.getText()));

    final Label titleLabel = lookup("#lbTitle").query();
    Assert.assertEquals("Unit", titleLabel.getText());

    final Label groupLabel = lookup("#lbGroup").query();
    Assert.assertEquals(1025, Integer.parseInt(groupLabel.getText()));

    final Label semesterLabel = lookup("#lbSemesters").query();
    Assert.assertEquals("1, 2", semesterLabel.getText());

    final Label tentativeLabel = lookup("#lbTentative").query();
    Assert.assertEquals("✗", tentativeLabel.getText());
  }

  @Test
  public void testTableContent() {
    final TableView<CourseTableEntry> courseTable = lookup("#courseTable").query();
    courses.forEach(course -> modules.forEach(module -> abstractUnits.forEach(abstractUnit -> {
      final char type;
      final HashSet<Integer> semesters = new HashSet<>();
      if (module.getId() == 1) {
        type = 'm';
      } else {
        type = 'e';
      }
      if (abstractUnit.getId() == 1) {
        semesters.addAll(Arrays.asList(1, 2));
      } else {
        semesters.addAll(Arrays.asList(3, 4));
      }
      final CourseTableEntry cte
          = new CourseTableEntry(course, module, abstractUnit, semesters, type);
      assertThat(courseTable.getItems(), hasItem(cte));
    })));
  }

  @Override
  public void start(final Stage stage) throws Exception {
    final Inflater inflater = new Inflater(new FXMLLoader());
    final Router router = new Router();

    final SessionDetailView sessionDetailView = new SessionDetailView(inflater, router);
    sessionDetailView.setSession(sessionFacade);

    stage.setScene(SceneFactory.create(sessionDetailView));
    stage.show();
  }
}

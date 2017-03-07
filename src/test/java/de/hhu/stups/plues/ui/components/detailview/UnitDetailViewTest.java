package de.hhu.stups.plues.ui.components.detailview;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.base.Joiner;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Group;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.data.entities.Unit;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class UnitDetailViewTest extends ApplicationTest {

  private static final String KEY = "U";
  private static final String TITLE = "Unit";
  private static final HashSet<Integer> SEMESTERS = new HashSet<>(Arrays.asList(1, 2));
  private final Unit unit;
  private final Set<AbstractUnit> abstractUnits;
  private final Set<Session> sessions;

  /**
   * Test constructor.
   */
  @SuppressWarnings("ResultOfMethodCallIgnored")
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
  public UnitDetailViewTest() {

    unit = mock(Unit.class, new ThrowsException(new RuntimeException()));
    final Group group = mock(Group.class, new ThrowsException(new RuntimeException()));
    final Session session1 = mock(Session.class, new ThrowsException(new RuntimeException()));
    final Session session2 = mock(Session.class, new ThrowsException(new RuntimeException()));

    final AbstractUnit au1 = mock(AbstractUnit.class, new ThrowsException(new RuntimeException()));
    final AbstractUnit au2 = mock(AbstractUnit.class, new ThrowsException(new RuntimeException()));

    this.abstractUnits = new HashSet<>(Arrays.asList(au1, au2));
    this.sessions = new HashSet<>(Arrays.asList(session1, session2));

    doReturn(123).when(group).getId();

    doReturn(1).when(session1).getId();
    doReturn(group).when(session1).getGroup();
    doReturn("mon").when(session1).getDay();
    doReturn(8).when(session1).getTime();

    doReturn(2).when(session2).getId();
    doReturn(group).when(session2).getGroup();
    doReturn("fri").when(session2).getDay();
    doReturn(8).when(session2).getTime();

    doReturn(sessions).when(group).getSessions();

    doReturn(KEY).when(unit).getKey();
    doReturn(TITLE).when(unit).getTitle();
    doReturn(SEMESTERS).when(unit).getSemesters();
    doReturn(abstractUnits).when(unit).getAbstractUnits();
    doReturn(new HashSet<>(Collections.singletonList(group))).when(unit).getGroups();

    doReturn("AU-1").when(au1).getKey();
    doReturn("Abstract Unit 1").when(au1).getTitle();
    doReturn("AU-2").when(au2).getKey();
    doReturn("Abstract Unit 2").when(au2).getTitle();
  }

  @Test
  public void testContentSize() {
    final TableView sessionsTable = lookup("#sessionTableView").query();
    Assert.assertEquals(sessions.size(), sessionsTable.getItems().size());

    final TableView abstractUnitTable = lookup("#abstractUnitTableView").query();
    Assert.assertEquals(abstractUnits.size(), abstractUnitTable.getItems().size());
  }

  @Test
  public void testUnitInfo() {
    final Label keyLabel = lookup("#key").query();
    Assert.assertEquals(KEY, keyLabel.getText());

    final Label titleLabel = lookup("#title").query();
    Assert.assertEquals(TITLE, titleLabel.getText());

    final Label semesterLabel = lookup("#semesters").query();
    Assert.assertEquals(Joiner.on(", ").join(SEMESTERS), semesterLabel.getText());
  }

  @Test
  public void testAbstractUnitTableContent() {
    final TableView<AbstractUnit> abstractUnitTableView = lookup("#abstractUnitTableView").query();
    abstractUnits.forEach(abstractUnit ->
        Assert.assertThat(abstractUnitTableView.getItems(), hasItem(abstractUnit)));
  }

  @Test
  public void testSessionsTableContent() {
    final TableView<Session> sessionTableView = lookup("#sessionTableView").query();
    sessions.forEach(session ->
        Assert.assertThat(sessionTableView.getItems(), hasItem(session)));
  }

  @Override
  public void start(final Stage stage) throws Exception {
    final Inflater inflater = new Inflater(new FXMLLoader());
    final Router router = new Router();

    final UnitDetailView unitDetailView = new UnitDetailView(inflater, router);
    unitDetailView.setUnit(unit);

    stage.setScene(SceneFactory.create(unitDetailView));
    stage.show();
  }
}

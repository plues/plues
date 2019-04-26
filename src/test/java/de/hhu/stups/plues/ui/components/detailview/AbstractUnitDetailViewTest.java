package de.hhu.stups.plues.ui.components.detailview;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.ModuleAbstractUnitSemester;
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
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class AbstractUnitDetailViewTest extends ApplicationTest {

  private static final String KEY = "P-PHIL-BTPEB";
  private static final String TITLE = "Abstract Unit";

  private final Set<Unit> units;
  private final Set<Module> modules;
  private final AbstractUnit abstractUnit;

  /**
   * Test constructor.
   */
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public AbstractUnitDetailViewTest() {
    abstractUnit = mock(AbstractUnit.class, new ThrowsException(new RuntimeException()));
    final Module mod1 = mock(Module.class, new ThrowsException(new RuntimeException()));
    final Module mod2 = mock(Module.class, new ThrowsException(new RuntimeException()));
    final Unit unit1 = mock(Unit.class, new ThrowsException(new RuntimeException()));
    final Unit unit2 = mock(Unit.class, new ThrowsException(new RuntimeException()));
    final ModuleAbstractUnitSemester maus1 =
        mock(ModuleAbstractUnitSemester.class, new ThrowsException(new RuntimeException()));
    final ModuleAbstractUnitSemester maus2 =
        mock(ModuleAbstractUnitSemester.class, new ThrowsException(new RuntimeException()));

    this.units = new HashSet<>(Arrays.asList(unit1, unit2));
    this.modules = new HashSet<>(Arrays.asList(mod1, mod2));

    doReturn(1).when(maus1).getSemester();
    doReturn(mod1).when(maus1).getModule();
    doReturn(abstractUnit).when(maus1).getAbstractUnit();

    doReturn(2).when(maus2).getSemester();
    doReturn(mod2).when(maus2).getModule();
    doReturn(abstractUnit).when(maus2).getAbstractUnit();

    doReturn("U1").when(unit1).getKey();
    doReturn("Unit 1").when(unit1).getTitle();
    doReturn("U2").when(unit2).getKey();
    doReturn("Unit 2").when(unit2).getTitle();

    doReturn(1).when(mod1).getPordnr();
    doReturn("Module 1").when(mod1).getTitle();
    doReturn(new HashSet<>(Arrays.asList(maus1, maus2)))
        .when(mod1).getModuleAbstractUnitSemesters();

    doReturn(2).when(mod2).getPordnr();
    doReturn("Module 2").when(mod2).getTitle();
    doReturn(new HashSet<>(Arrays.asList(maus1, maus2)))
        .when(mod2).getModuleAbstractUnitSemesters();

    doReturn(new HashSet<>()).when(mod1).getModuleAbstractUnitTypes();
    doReturn(new HashSet<>()).when(mod2).getModuleAbstractUnitTypes();

    doReturn(KEY).when(abstractUnit).getKey();
    doReturn(TITLE).when(abstractUnit).getTitle();
    doReturn(units).when(abstractUnit).getUnits();
    doReturn(modules).when(abstractUnit).getModules();
  }

  @Test
  public void testContentSize() {
    final TableView tableViewUnits = lookup("#tableViewUnits").query();
    Assert.assertEquals(units.size(), tableViewUnits.getItems().size());

    final TableView tableViewModules = lookup("#tableViewModules").query();
    Assert.assertEquals(modules.size(), tableViewModules.getItems().size());
  }

  @Test
  public void testAbstractUnitInfo() {
    final Label keyLabel = lookup("#key").query();
    Assert.assertEquals(KEY, keyLabel.getText());

    final Label titleLabel = lookup("#title").query();
    Assert.assertEquals(TITLE, titleLabel.getText());
  }

  @Test
  public void testUnitTableContent() {
    final TableView<Unit> unitTableView = lookup("#tableViewUnits").query();
    units.forEach(unit -> Assert.assertThat(unitTableView.getItems(), hasItem(unit)));
  }

  @Test
  public void testModuleTableContent() {
    final TableView<Module> moduleTableView = lookup("#tableViewModules").query();
    modules.forEach(module -> Assert.assertThat(moduleTableView.getItems(), hasItem(module)));
  }

  @Override
  public void start(final Stage stage) throws Exception {
    final Inflater inflater = new Inflater(new FXMLLoader());
    final Router router = new Router();

    final AbstractUnitDetailView abstractUnitDetailView =
        new AbstractUnitDetailView(inflater, router);
    abstractUnitDetailView.setAbstractUnit(abstractUnit);

    stage.setScene(SceneFactory.create(abstractUnitDetailView));
    stage.show();
  }
}

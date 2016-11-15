package de.hhu.stups.plues.ui.components;

import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testfx.api.FxAssert.verifyThat;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.services.SolverService;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.tasks.SolverTask;
import de.hhu.stups.plues.ui.layout.Inflater;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.matcher.control.LabeledMatchers;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public abstract class ResultBoxTest extends ApplicationTest {
  private final Course major;
  private final Course minor;
  private Text icon;
  private PdfRenderingTask task;

  /**
   * Default constructor.
   */
  @SuppressWarnings("WeakerAccess")
  public ResultBoxTest() {
    this.major = new Course();
    major.setLongName("Major Course");
    major.setDegree("MA");
    major.setKzfa("H");
    major.setPo(2016);

    this.minor = new Course();
    minor.setLongName("Minor Course");
    minor.setDegree("MI");
    minor.setKzfa("N");
    minor.setPo(2013);
  }

  @Before
  public void sleepBeforeTests() {
    this.sleep(500, TimeUnit.MILLISECONDS); // sleep briefly for Task to finish
  }

  @Test
  public void majorLabel() {
    verifyThat("#lbMajor", LabeledMatchers.hasText(major.getFullName()));
  }

  @Test
  public void minorLabel() {
    verifyThat("#lbMinor", LabeledMatchers.hasText(minor.getFullName()));
  }

  @Test
  public void testIcon() {
    final Text mark = this.icon;

    final Label icon = lookup("#lbIcon").query();
    final Text graphic = (Text) icon.getGraphic();
    Assert.assertEquals(mark.getText(), graphic.getText());
  }

  @Override
  @SuppressWarnings("unchecked")
  public void start(final Stage stage) throws Exception {
    final SolverService solverService = mock(SolverService.class);
    when(solverService.computeFeasibilityTask(anyVararg())).thenReturn(mock(SolverTask.class));

    final Delayed<SolverService> solver = new Delayed<>();
    final Inflater inflater = new Inflater(new FXMLLoader());

    solver.set(solverService);
    final ResultBox resultBox = new ResultBox(
        inflater, solver, (major1, minor1, solverTask) -> task,
        Executors.newSingleThreadExecutor(), major, minor, new VBox());

    final Scene scene = new Scene(resultBox, 200, 200);
    stage.setScene(scene);
    stage.show();
  }

  void setIcon(final Text icon) {
    this.icon = icon;
  }

  void setTask(final PdfRenderingTask task) {
    this.task = task;
  }
}

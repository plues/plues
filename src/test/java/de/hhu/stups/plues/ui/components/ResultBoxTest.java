package de.hhu.stups.plues.ui.components;

import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.testfx.api.FxAssert.verifyThat;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;

import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
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
  private Course major;
  private Course minor;
  private Text icon;
  private PdfRenderingTask task;

  /**
   * Default constructor.
   */
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
    verifyThat("#major", LabeledMatchers.hasText(major.getFullName()));
  }

  @Test
  public void minorLabel() {
    verifyThat("#minor", LabeledMatchers.hasText(minor.getFullName()));
  }

  @Test
  public void testIcon() {
    final Text mark = this.icon;

    final Label icon = lookup("#icon").query();
    final Text graphic = (Text) icon.getGraphic();
    Assert.assertEquals(mark.getText(), graphic.getText());
  }

  @Override
  @SuppressWarnings("unchecked")
  public void start(final Stage stage) throws Exception {
    final SolverService solverService = mock(SolverService.class);
    when(solverService.computeFeasibilityTask(anyVararg())).thenReturn(mock(SolverTask.class));

    Delayed<SolverService> solver = new Delayed<>();
    solver.set(solverService);
    final ResultBox resultBox = new ResultBox(
        new FXMLLoader(), solver, (major1, minor1, solverTask) -> task,
        Executors.newSingleThreadExecutor(), major, minor, new VBox());

    final Scene scene = new Scene(resultBox, 200, 200);
    stage.setScene(scene);
    stage.show();
  }

  void setIcon(final Text icon) {
    this.icon = icon;
  }

  public void setTask(PdfRenderingTask task) {
    this.task = task;
  }
}

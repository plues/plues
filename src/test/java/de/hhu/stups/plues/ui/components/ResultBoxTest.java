package de.hhu.stups.plues.ui.components;

import com.google.inject.assistedinject.Assisted;

import static org.testfx.api.FxAssert.verifyThat;

import de.hhu.stups.plues.Delayed;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.hhu.stups.plues.tasks.PdfRenderingTaskFactory;
import de.hhu.stups.plues.tasks.SolverService;
import de.hhu.stups.plues.tasks.SolverTask;

import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.matcher.control.LabeledMatchers;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

public abstract class ResultBoxTest extends ApplicationTest {
  private Course major;
  private Course minor;
  private Text icon;
  private HashMap<String, Boolean> enabledButtons;

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
  public void enabledButtons() {
    for (final Map.Entry<String, Boolean> entry : enabledButtons.entrySet()) {
      final Button button = lookup("#" + entry.getKey()).query();
      Assert.assertNotEquals(button.disableProperty().get(), entry.getValue());
    }
  }

  @Test
  public void testIcon() {
    final Text mark = this.icon;

    final Label icon = lookup("#icon").query();
    final Text graphic = (Text) icon.getGraphic();
    Assert.assertEquals(mark.getText(), graphic.getText());
  }

  @Override
  public void start(final Stage stage) throws Exception {
    FXMLLoader loader = new FXMLLoader();
    loader.setBuilderFactory(type -> {
      if (type.equals(PdfButtonBar.class)) {
        return () -> new PdfButtonBar(new FXMLLoader());
      }
      return new JavaFXBuilderFactory().getBuilder(type);
    });

    final ResultBox resultBox = new ResultBox(loader, new Delayed<SolverService>(),
      (major1, minor1, solverTask) -> null, Executors.newSingleThreadExecutor(), major, minor);

    final Scene scene = new Scene(resultBox, 200, 200);
    stage.setScene(scene);
    stage.show();
  }

  void setIcon(final Text icon) {
    this.icon = icon;
  }

  void setEnabledButtons(final HashMap<String, Boolean> enabledButtons) {
    this.enabledButtons = enabledButtons;
  }
}

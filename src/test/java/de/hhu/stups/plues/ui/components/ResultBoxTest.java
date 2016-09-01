package de.hhu.stups.plues.ui.components;

import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.tasks.PdfRenderingTask;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.matcher.control.LabeledMatchers;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import static org.testfx.api.FxAssert.verifyThat;



public abstract class ResultBoxTest extends ApplicationTest {
  private Course major;
  private Course minor;
  private PdfRenderingTask task;
  private Text icon;

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

  private Course getMajor() {
    return major;
  }

  private void setMajor(final Course major) {
    this.major = major;
  }

  private Course getMinor() {
    return minor;
  }

  private void setMinor(final Course minor) {
    this.minor = minor;
  }

  private PdfRenderingTask getTask() {
    return task;
  }

  void setTask(final PdfRenderingTask task) {
    this.task = task;
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
  public void start(final Stage stage) throws Exception {
    final ResultBox resultBox = new ResultBox(new FXMLLoader(),
        (major1, minor1) -> task, Executors.newSingleThreadExecutor(), major, minor, new VBox());

    final Scene scene = new Scene(resultBox, 200, 200);
    stage.setScene(scene);
    stage.show();
  }

  void setIcon(final Text icon) {
    this.icon = icon;
  }
}

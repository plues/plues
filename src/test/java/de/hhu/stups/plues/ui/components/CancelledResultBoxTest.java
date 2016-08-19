package de.hhu.stups.plues.ui.components;

import static org.testfx.api.FxAssert.verifyThat;

import de.hhu.stups.plues.data.entities.Course;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.matcher.control.LabeledMatchers;

import java.nio.file.Path;

@RunWith(JUnit4.class)
public class CancelledResultBoxTest extends ApplicationTest {

  private Course major;
  private Course minor;

  @Test
  public void cancelledIcon() {
    final Text mark = FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.QUESTION, "50");

    final Label icon = lookup("#icon").query();
    final Text graphic = (Text) icon.getGraphic();
    Assert.assertEquals(mark.getText(), graphic.getText());
  }

  @Test
  public void majorLabel() {
    verifyThat("#major", LabeledMatchers.hasText(major.getFullName()));
  }

  @Test
  public void minorLabel() {
    verifyThat("#minor", LabeledMatchers.hasText(minor.getFullName()));
  }

  @Override
  public void start(final Stage stage) throws Exception {
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

    final PdfRenderingService service = new TestPdfService();

    final ResultBox resultBox = new ResultBox(new FXMLLoader(), service, major, minor);

    final Scene scene = new Scene(resultBox, 200, 200);
    stage.setScene(scene);
    stage.show();
  }

  static class TestPdfService extends PdfRenderingService {

    TestPdfService() {
      super(null, null, null);
    }

    public void start() {
      this.cancel();
    }

    @Override
    protected Task<Path> createTask() {
      return null;
    }
  }
}

package de.hhu.stups.plues.ui.components;

import de.hhu.stups.plues.data.entities.Course;

import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import java.nio.file.Path;

import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class PdfButtonBarTest extends ApplicationTest {
  private Course major;
  private Course minor;

  /**
   * Default constructor.
   */
  public PdfButtonBarTest () {
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

  @Test
  public void testExistence () {

  }

  @Override
  public void start(Stage stage) throws Exception {
    final PdfButtonBar bar = new PdfButtonBar(new FXMLLoader());
    bar.setMajor(major);
    bar.setMinor(minor);
    bar.setTask(new Task<Path>() {
      @Override
      protected Path call() throws Exception {
        return null;
      }
    });

    final Scene scene = new Scene(bar, 200, 200);
    stage.setScene(scene);
    stage.show();
  }
}

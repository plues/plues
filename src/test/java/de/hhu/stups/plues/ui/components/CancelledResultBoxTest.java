package de.hhu.stups.plues.ui.components;

import de.hhu.stups.plues.tasks.PdfRenderingTask;
import org.controlsfx.glyphfont.FontAwesome;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.file.Path;

@RunWith(JUnit4.class)
public class CancelledResultBoxTest extends ResultBoxTest {

  /**
   * Default constructor.
   */
  public CancelledResultBoxTest() {
    super();
    this.setTask(new TestPdfTask());
    FontAwesome.Glyph glyph = FontAwesome.Glyph.QUESTION;
    glyph.setFontSize(50);
    this.setIcon(glyph);
  }


  static class TestPdfTask extends PdfRenderingTask {

    TestPdfTask() {
      super(null, null, null, null, null);
    }

    public Path call() {
      this.cancel();
      return null;
    }
  }
}

package de.hhu.stups.plues.ui.components;

import de.hhu.stups.plues.tasks.PdfRenderingTask;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;

import java.nio.file.Path;
import java.nio.file.Paths;


public class SuccessfulResultBoxTest extends ResultBoxTest {

  /**
   * Default constructor.
   */
  public SuccessfulResultBoxTest() {
    super();
    this.setTask(new TestPdfTask());
    Glyph glyph = new Glyph("FontAwesome", FontAwesome.Glyph.CHECK);
    glyph.setFontSize(50);
    this.setIcon(glyph);
  }

  private static final class TestPdfTask extends PdfRenderingTask {
    TestPdfTask() {
      super(null, null, null, null,null);
    }

    public Path call() {
      return Paths.get(".");
    }
  }
}

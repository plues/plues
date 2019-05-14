package de.hhu.stups.plues.ui.components;

import de.hhu.stups.plues.tasks.PdfRenderingTask;
import org.controlsfx.glyphfont.FontAwesome;

public class FailureResultBoxTest extends ResultBoxTest {

  /**
   * Default constructor.
   */
  public FailureResultBoxTest() {
    super();
    this.setTask(new TestPdfTask());
    FontAwesome.Glyph glyph = FontAwesome.Glyph.REMOVE;
    glyph.setFontSize(50);
    this.setIcon(glyph);
  }

  private static final class TestPdfTask extends PdfRenderingTask {
    TestPdfTask() {
      super(null, null, null, null, null);
    }
  }
}

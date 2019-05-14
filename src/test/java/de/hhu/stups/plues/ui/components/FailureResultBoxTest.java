package de.hhu.stups.plues.ui.components;

import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;

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

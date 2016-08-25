package de.hhu.stups.plues.ui.components;

import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;

import java.util.HashMap;

public class FailureResultBoxTest extends ResultBoxTest {

  /**
   * Default constructor.
   */
  public FailureResultBoxTest() {
    super();
    this.setTask(new TestPdfTask());
    this.setIcon(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.REMOVE, "50"));
    HashMap<String, Boolean> buttons = new HashMap<>();
    buttons.put("show", false);
    buttons.put("save", false);
    buttons.put("cancel", false);
    this.setEnabledButtons(buttons);
  }

  private static final class TestPdfTask extends PdfRenderingTask {
    TestPdfTask() {
      super(null, null, null, null);
    }
  }
}

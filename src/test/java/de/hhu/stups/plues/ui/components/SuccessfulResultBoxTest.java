package de.hhu.stups.plues.ui.components;

import de.hhu.stups.plues.tasks.PdfRenderingTask;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;

import java.util.HashMap;

public class SuccessfulResultBoxTest extends ResultBoxTest {

  /**
   * Default constructor.
   */
  public SuccessfulResultBoxTest() {
    super();
    this.setTask(new TestPdfTask());
    this.setIcon(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.CHECK, "50"));
    HashMap<String, Boolean> buttons = new HashMap<>();
    buttons.put("show", true);
    buttons.put("save", true);
    buttons.put("cancel", false);
    this.setEnabledButtons(buttons);
  }

  private static final class TestPdfTask extends PdfRenderingTask {
    TestPdfTask() {
      super(null, null);
    }

  }
}

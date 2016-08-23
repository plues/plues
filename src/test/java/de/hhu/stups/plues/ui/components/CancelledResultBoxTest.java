package de.hhu.stups.plues.ui.components;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import javafx.concurrent.Task;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


@RunWith(JUnit4.class)
public class CancelledResultBoxTest extends ResultBoxTest {

  /**
   * Default constructor.
   */
  public CancelledResultBoxTest() {
    super();
    this.setService(new TestPdfService());
    this.setIcon(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.QUESTION, "50"));
    HashMap<String, Boolean> buttons = new HashMap<>();
    buttons.put("show", false);
    buttons.put("save", false);
    buttons.put("cancel", false);
    this.setEnabledButtons(buttons);
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

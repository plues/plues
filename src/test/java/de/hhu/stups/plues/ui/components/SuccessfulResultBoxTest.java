package de.hhu.stups.plues.ui.components;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;
import javafx.concurrent.Task;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SuccessfulResultBoxTest extends ResultBoxTest {

  private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

  /**
   * Default constructor.
   */
  public SuccessfulResultBoxTest() {
    super();
    this.setService(new TestPdfService());
    this.setIcon(FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.CHECK, "50"));
  }

  private static final class TestPdfService extends PdfRenderingService {
    TestPdfService() {
      super(null, null, EXECUTOR);
    }

    @Override
    public Task<Path> createTask() {
      return new Task<Path>() {
        @Override
        protected Path call() throws Exception {
          return Paths.get(".");
        }
      };
    }
  }
}

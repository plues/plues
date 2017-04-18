package de.hhu.stups.plues.studienplaene;

import static org.junit.Assert.assertNotNull;

import de.hhu.stups.plues.ui.exceptions.RenderingException;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;


public class TestRenderer extends TestBase {

  /**
   * Test setup.
   */
  @Before
  public void setUp() throws URISyntaxException {
    super.setUp();
  }

  @Test
  public void testItWorksForColor() throws RenderingException, IOException {
    final ColorScheme colorScheme = new ColorScheme("Something",
        new HashSet<>(Arrays.asList("#DCBFBE", "#DCD6BE", "#C1DCBE", "#F1EAB4", "#C5CBF1",
            "#EFF1CB", "#E5CBF1", "#DCF1E9", "#EFB9B9", "#FFA6A6", "#FCFE80", "#C7FF72",
            "#9AFFA4", "#9AFFD6", "#9AFFF9", "#94E5FF", "#A4C1FF", "#CFA4FF", "#F2A4FF",
            "#F6CCFF", "#FFB5F0", "#F7D9E4", "#E78FFB", "#DAFFB4", "#B4FFFD", "#69BCFF",
            "#FFA361")));
    final Renderer renderer = new Renderer(store, result, course, colorScheme);
    final ByteArrayOutputStream result = renderer.getResult();

    final File pdf = File.createTempFile("color", ".pdf");
    try (FileOutputStream outputStream = new FileOutputStream(pdf.getAbsoluteFile())) {
      result.writeTo(outputStream);
    }
    assertNotNull(result);
  }

  @Test
  public void testItWorksForGrayscale() throws RenderingException, IOException {
    final ColorScheme colorScheme = new ColorScheme("grayscale", new HashSet<>());
    final Renderer renderer = new Renderer(store, result, course, colorScheme);
    final ByteArrayOutputStream result = renderer.getResult();

    final File pdf = File.createTempFile("gray", ".pdf");
    try (FileOutputStream stream = new FileOutputStream(pdf.getAbsoluteFile())) {
      result.writeTo(stream);
    }
    assertNotNull(result);
  }
}

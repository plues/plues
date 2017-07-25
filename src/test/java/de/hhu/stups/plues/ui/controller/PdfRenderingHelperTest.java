package de.hhu.stups.plues.ui.controller;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.io.Files;

import javafx.beans.property.SimpleStringProperty;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.MissingResourceException;

public class PdfRenderingHelperTest {

  private final File tempDir = Files.createTempDir();
  private Path fileToSavePath;

  /**
   * Copy test file to temporary folder.
   */
  @Before
  public void setup() throws IOException {
    try (final InputStream fileStream = getClass().getResourceAsStream("/test.pdf")) {
      final String fileToSave = "/test.pdf";
      if (fileStream == null) {
        throw new MissingResourceException("Could not find test file resource.",
          this.getClass().getName(), fileToSave);
      }
      fileToSavePath = Paths.get(tempDir.toPath().toString() + fileToSave);
      java.nio.file.Files.copy(fileStream, fileToSavePath);
    }
  }

  /**
   * Simple test to save a pdf file.
   */
  @Test
  public void testSavePdfFile() throws IOException {
    final String pathString = fileToSavePath.toString();
    final Path newPath = Paths.get(pathString.substring(0, pathString.lastIndexOf('/') + 1)
        .concat("test2.pdf"));
    PdfRenderingHelper.savePdf(fileToSavePath, newPath, new SimpleStringProperty());
    final File checkFile = new File(newPath.toString());
    assertTrue(checkFile.exists());
    assertFalse(checkFile.isDirectory());
  }

  /**
   * Simple test so save an pdf file by replacing another file.
   */
  @Test
  public void testSaveAndReplaceExistingPdfFile() throws IOException {
    final String pathString = fileToSavePath.toString();
    PdfRenderingHelper.savePdf(fileToSavePath, fileToSavePath, new SimpleStringProperty());
    final File checkFile = new File(pathString);
    assertTrue(checkFile.exists());
    assertFalse(checkFile.isDirectory());
  }
}

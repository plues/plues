package de.hhu.stups.plues.ui.components;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import javax.swing.SwingUtilities;

public interface OpenFileHandler {

  /**
   * Try to open a given file, e.g., a .pdf file.
   */
  static void tryOpenFile(final File file) {
    SwingUtilities.invokeLater(() -> {
      try {
        Desktop.getDesktop().open(file);
      } catch (final IOException exc) {
        final Logger logger = LoggerFactory.getLogger("openFileHandler");
        logger.error("Exception while opening file", exc);
      }
    });
  }
}

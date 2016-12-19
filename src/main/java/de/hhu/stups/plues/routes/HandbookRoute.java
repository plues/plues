package de.hhu.stups.plues.routes;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

import javax.swing.SwingUtilities;

public class HandbookRoute implements Route {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final Properties properties;

  @Inject
  HandbookRoute(final Properties properties) {
    this.properties = properties;
  }

  @Override
  public void transition(final Object... args) {
    final String handbook = "doc/handbook.html";
    final ClassLoader classLoader = this.getClass().getClassLoader();

    try (final InputStream stream = classLoader.getResourceAsStream(handbook)) {
      // if we didn't find the handbook in the resources try opening online version
      if (stream == null) {
        final String url = this.properties.getProperty("handbook-url");

        // open url in browser
        SwingUtilities.invokeLater(() -> {
          try {
            Desktop.getDesktop().browse(new URI(url));
          } catch (IOException | URISyntaxException exception) {
            logger.error("browsing to handbook" + handbook, exception);
          }
        });
        return;
      }
      //
      // if we found the handbook, move it to a temporary location and open it from there
      final Path output = Files.createTempFile("Handbook", ".html");
      output.toFile().deleteOnExit();
      Files.copy(stream, output, StandardCopyOption.REPLACE_EXISTING);

      SwingUtilities.invokeLater(() -> {
        try {
          Desktop.getDesktop().open(output.toFile());
        } catch (final IOException exception) {
          logger.error("showing " + handbook, exception);
        }
      });
    } catch (final IOException exception) {
      logger.error("showHandbook", exception);
    }

  }
}

package de.hhu.stups.plues.routes;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

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
  private final Format format;

  @Inject
  HandbookRoute(final Properties properties, @Assisted final Format format) {
    this.properties = properties;
    this.format = format;
  }

  @SuppressWarnings({"NP_LOAD_OF_KNOWN_NULL_VALUE","RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE",
    "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE"})
  @Override
  public void transition(final RouteNames routeName, final Object... args) {
    final String handbook = this.format.getFileName();
    final ClassLoader classLoader = this.getClass().getClassLoader();

    try (final InputStream stream = classLoader.getResourceAsStream(handbook)) {
      // if we didn't find the handbook in the resources try opening online version
      if (stream == null) {
        final String url = this.properties.getProperty(this.format.getPropertyName());

        // open url in browser
        SwingUtilities.invokeLater(() -> openUrl(handbook, url));
        return;
      }
      //
      // if we found the handbook, move it to a temporary location and open it from there
      final Path output = Files.createTempFile("Handbook", this.format.getExtension());
      output.toFile().deleteOnExit();
      Files.copy(stream, output, StandardCopyOption.REPLACE_EXISTING);

      SwingUtilities.invokeLater(() -> openFile(handbook, output));
    } catch (final IOException exception) {
      logger.error("showHandbook", exception);
    }

  }

  private void openFile(final String handbook, final Path output) {
    try {
      Desktop.getDesktop().open(output.toFile());
    } catch (final IOException exception) {
      logger.error("showing " + handbook, exception);
    }
  }

  private void openUrl(final String handbook, final String url) {
    try {
      Desktop.getDesktop().browse(new URI(url));
    } catch (IOException | URISyntaxException exception) {
      logger.error("browsing to handbook" + handbook, exception);
    }
  }

  public enum Format {
    HTML(".html"), PDF(".pdf");

    private final String extension;

    Format(final String extension) {
      this.extension = extension;
    }

    String getExtension() {
      return extension;
    }

    String getFileName() {
      return "doc/handbook" + this.getExtension();
    }


    private String getPropertyName() {
      switch (this) {
        case HTML:
          return "handbook-url-html";
        case PDF:
        default:
          return "handbook-url-pdf";
      }
    }
  }
}

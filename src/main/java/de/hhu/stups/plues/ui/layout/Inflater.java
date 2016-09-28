package de.hhu.stups.plues.ui.layout;

import com.google.inject.Inject;

import de.hhu.stups.plues.ui.exceptions.InflaterException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Inflates a layout from a filename.
 */
public class Inflater {

  private final FXMLLoader loader;
  private static final String DEFAULT_BUNDLE = "main";

  @Inject
  public Inflater(final FXMLLoader loader) {
    this.loader = loader;
  }

  /**
   * Inflate a fxml resource as a layout from <tt>/fxml/</tt>.
   *
   * @param name The name of the xml file without the <tt>.fxml</tt> extension.
   * @param bundleName The name of the used bundle
   * @return {@link Parent}
   */
  public Parent inflate(final String name, final String bundleName) {
    return inflate(name, null, null, bundleName);
  }

  /**
   * Inflate without root and controller and resource bundle name.
   * @param name The name of the fxml file without the <tt>.fxml</tt> extension.
   * @return {@link Parent}
   */
  public Parent inflate(final String name) {
    return inflate(name, null, null, DEFAULT_BUNDLE);
  }

  /**
   * Inflate wiht default bundle name.
   * @param name The name of the fxml file without the <tt>.fxml</tt> extension.
   * @param root optional root node to inflate this layout into
   * @param controller controller for the fxml file
   * @return {@link Parent}
   */
  public Parent inflate(final String name, final Parent root, final Object controller) {
    return inflate(name, root, controller, DEFAULT_BUNDLE);
  }

  /**
   * Inflate a fxml resource as a layout from <tt>/fxml/</tt>.
   *
   * @param name The name of the fxml file without the <tt>.fxml</tt> extension.
   * @param root optional root node to inflate this layout into
   * @param controller controller for the fxml file
   * @param bundleName Name of the i18n resource to bind
   */
  public Parent inflate(final String name, final Parent root,
                        final Object controller, final String bundleName) {
    // set location explicitly to ensure using the injected fxml loader
    loader.setLocation(getClass().getResource("/fxml/" + name + ".fxml"));

    if (root != null) {
      loader.setRoot(root);
    }

    if (controller != null) {
      loader.setController(controller);
    }

    ResourceBundle bundle = ResourceBundle.getBundle("lang." + bundleName, new Locale("de"));
    loader.setResources(bundle);

    try {
      return loader.load();
    } catch (final IOException ignored) {
      final Logger logger = Logger.getLogger(getClass().getSimpleName());
      logger.log(Level.SEVERE, "Exception in FXML Loader", ignored);
      throw new InflaterException(ignored);
    }
  }
}

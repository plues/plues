package de.hhu.stups.plues.ui.layout;

import com.google.inject.Inject;

import de.hhu.stups.plues.ui.exceptions.InflaterException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.hibernate.annotations.common.util.impl.LoggerFactory;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Inflates a layout from a filename.
 */
public class Inflater {

  private final FXMLLoader loader;
  private static final ResourceBundle MAIN_BUNDLE = ResourceBundle.getBundle("lang.main");

  @Inject
  public Inflater(final FXMLLoader loader) {
    this.loader = loader;
  }

  /**
   * Inflate a fxml resource as a layout from <tt>/fxml/</tt>.
   *
   * @param name The name of the xml file without the <tt>.fxml</tt> extension.
   * @param bundleNames The name of the used bundles. Order of bundles comparable to MRO: First
   *                    specific bundles and later common ones.
   * @return {@link Parent}
   */
  public Parent inflate(final String name, final String... bundleNames) {
    return inflate(name, null, null, bundleNames);
  }

  /**
   * Inflate without root and controller and resource bundle name.
   * @param name The name of the fxml file without the <tt>.fxml</tt> extension.
   * @return {@link Parent}
   */
  public Parent inflate(final String name) {
    return inflate(name, null, null, new String[0]);
  }

  /**
   * Inflate with default bundle name.
   * @param name The name of the fxml file without the <tt>.fxml</tt> extension.
   * @param root optional root node to inflate this layout into
   * @param controller controller for the fxml file
   * @return {@link Parent}
   */
  public Parent inflate(final String name, final Parent root, final Object controller) {
    return inflate(name, root, controller, new String[0]);
  }

  /**
   * Inflate a fxml resource as a layout from <tt>/fxml/</tt>.
   *
   * @param name The name of the fxml file without the <tt>.fxml</tt> extension.
   * @param root optional root node to inflate this layout into
   * @param controller controller for the fxml file
   * @param bundleNames Name of the i18n resources to bind. Order of bundles comparable to MRO:
   *                    First specific bundles and later common ones.
   */
  public Parent inflate(final String name, final Parent root,
                        final Object controller, final String... bundleNames) {
    // set location explicitly to ensure using the injected fxml loader
    loader.setLocation(getClass().getResource("/fxml/" + name + ".fxml"));

    if (root != null) {
      loader.setRoot(root);
    }

    if (controller != null) {
      loader.setController(controller);
    }

    final ResourceBundle[] bundles = new ResourceBundle[bundleNames.length + 1];
    for (int i = 0;i < bundleNames.length; i++) {
      bundles[i] = ResourceBundle.getBundle("lang." + bundleNames[i]);
    }
    bundles[bundleNames.length] = MAIN_BUNDLE;

    loader.setResources(new CustomMultiResourceBundle(bundles));

    try {
      return loader.load();
    } catch (final IOException ignored) {
      final Logger logger = LoggerFactory.logger(getClass());
      logger.error("Exception in FXML Loader", ignored);
      throw new InflaterException(ignored);
    }
  }

  private static final class CustomMultiResourceBundle extends ResourceBundle {

    private final ResourceBundle[] resourceBundles;

    CustomMultiResourceBundle(final ResourceBundle... resourceBundles) {
      this.resourceBundles = resourceBundles;
    }

    @Override
    protected Object handleGetObject(final String key) {
      for (final ResourceBundle resourceBundle : resourceBundles) {
        if (!resourceBundle.containsKey(key)) {
          continue;
        }
        return resourceBundle.getString(key);
      }

      return null;
    }

    @Override
    public Enumeration<String> getKeys() {
      final Set<String> allKeys =
          Arrays.stream(resourceBundles).flatMap(resourceBundle
              -> resourceBundle.keySet().stream()).collect(Collectors.toSet());

      return Collections.enumeration(allKeys);
    }
  }
}

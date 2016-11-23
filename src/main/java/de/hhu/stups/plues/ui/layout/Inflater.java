package de.hhu.stups.plues.ui.layout;

import com.google.inject.Inject;

import de.hhu.stups.plues.ui.exceptions.InflaterException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Inflates a layout from a filename.
 */
public class Inflater {

  private final FXMLLoader loader;
  private static final Locale LOCALE = Locale.getDefault();
  private static final ResourceBundle MAIN_BUNDLE = ResourceBundle.getBundle("lang.main", LOCALE);

  @Inject
  public Inflater(final FXMLLoader loader) {
    this.loader = loader;
  }

  /**
   * Inflate a fxml resource as a layout from <tt>/fxml/</tt>.
   *
   * @param name The name of the xml file without the <tt>.fxml</tt> extension.
   * @param bundleNames The name of the used bundles
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
   * @param bundleNames Name of the i18n resources to bind
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

    ResourceBundle[] bundles = new ResourceBundle[bundleNames.length + 1];
    for (int i = 0;i < bundleNames.length; i++) {
      bundles[i] = ResourceBundle.getBundle("lang." + bundleNames[i], LOCALE);
    }
    bundles[bundleNames.length] = MAIN_BUNDLE;

    loader.setResources(new CustomMultiResourceBundle(bundles));

    try {
      return loader.load();
    } catch (final IOException ignored) {
      final Logger logger = Logger.getLogger(getClass().getSimpleName());
      logger.log(Level.SEVERE, "Exception in FXML Loader", ignored);
      throw new InflaterException(ignored);
    }
  }

  private static final class CustomMultiResourceBundle extends ResourceBundle {

    private final ResourceBundle[] resourceBundles;

    public CustomMultiResourceBundle(final ResourceBundle... resourceBundles) {
      this.resourceBundles = resourceBundles;
    }

    @Override
    protected Object handleGetObject(String key) {
      for (ResourceBundle resourceBundle : resourceBundles) {
        if (!resourceBundle.containsKey(key)) {
          continue;
        }
        return resourceBundle.getString(key);
      }

      return null;
    }

    @Override
    public Enumeration<String> getKeys() {
      Set<String> allKeys = new HashSet<>();
      Set<Set<String>> keySets =
          Arrays.stream(resourceBundles).map(ResourceBundle::keySet).collect(Collectors.toSet());
      keySets.forEach(allKeys::addAll);

      return Collections.enumeration(allKeys);
    }
  }
}

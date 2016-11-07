package de.hhu.stups.plues.injector;

import static java.lang.Thread.currentThread;

import com.google.inject.AbstractModule;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

class PropertiesModule extends AbstractModule {

  private static Properties setupProperties() {
    final Properties defaults = new Properties();
    final Properties properties = loadProperties(defaults, "main", "local");

    // settings that can be overridden in env vars
    final String[] env = new String[] {"MODELPATH", "DBPATH"};

    for (final String it : env) {
      if (System.getenv(it) != null) {
        properties.setProperty(it.toLowerCase(), System.getenv(it));
      }
    }
    return properties;
  }

  private static Properties loadProperties(final Properties properties,
                                           final String... propertyFiles) {
    final Logger logger = Logger.getLogger(PropertiesModule.class.getSimpleName());
    for (final String propertyFile : propertyFiles) {
      try {
        final ClassLoader classLoader = currentThread().getContextClassLoader();
        final InputStream p = classLoader.getResourceAsStream(propertyFile + ".properties");

        if (p == null) {
          continue;
        }

        properties.load(p);
      } catch (final IOException exception) {
        logger.log(Level.SEVERE, propertyFile + ".properties produced IO Error!", exception);
      }
    }
    return properties;
  }

  @Override
  public void configure() {
    bind(Properties.class).toInstance(setupProperties());
  }
}

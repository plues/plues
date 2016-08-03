package de.hhu.stups.plues.injector;

import com.google.inject.AbstractModule;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static java.lang.Thread.currentThread;

class PropertiesModule extends AbstractModule {
    private static Properties setupProperties() {
        Properties defaults = new Properties();
        Properties properties = loadProperties(defaults, "main", "local");

        // settings that can be overriden in env vars
        String[] env = new String[]{"MODELPATH", "DBPATH"};

        for (String it : env) {
            if (System.getenv(it) != null) {
                properties.setProperty(it.toLowerCase(), System.getenv(it));
            }
        }
        return properties;
    }

    private static Properties loadProperties(Properties properties, String... propertyFiles) {
        for (String propertyFile : propertyFiles) {
            try {
                InputStream p = currentThread().getContextClassLoader().getResourceAsStream(propertyFile + ".properties");
                if (p == null) {
                    continue;
                }
                properties.load(p);
            } catch (FileNotFoundException e) {
                System.err.println(propertyFile + ".properties is missing!");
            } catch (IOException e) {
                System.err.println(propertyFile + ".properties produced IO Error!");
            }
        }
        return properties;
    }


    @Override
    public void configure() {
        bind(Properties.class).toInstance(setupProperties());
    }
}

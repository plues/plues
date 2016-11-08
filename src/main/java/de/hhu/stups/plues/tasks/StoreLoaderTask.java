package de.hhu.stups.plues.tasks;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.Helpers;
import de.hhu.stups.plues.ObservableStore;
import de.hhu.stups.plues.data.IncompatibleSchemaError;
import de.hhu.stups.plues.data.SqliteStore;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.StoreException;

import javafx.concurrent.Task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StoreLoaderTask extends Task<Store> {
  private final Logger logger = Logger.getLogger(getClass().getSimpleName());

  private static final long MAX_STEPS = 3;
  private static final String PLUES = "plues";
  private static final String EXTENSION = ".sqlite3";
  private final ResourceBundle resources;
  private final Properties properties;

  private Path dbWorkingPath;
  private final String path;

  /**
   * Constuctor to create store loader task.
   * @param storePath Path where to find store
   */
  @Inject
  public StoreLoaderTask(final Properties properties, @Assisted final String storePath) {
    this.properties = properties;
    this.path = storePath;
    this.resources = ResourceBundle.getBundle("lang.tasks");
    updateTitle(resources.getString("dbTitle"));
  }

  @Override
  protected final Store call() throws Exception {
    checkExportDatabase();
    try {
      return new ObservableStore(new SqliteStore(dbWorkingPath.toString()));
    } catch (IncompatibleSchemaError | StoreException exception) {
      logger.log(Level.SEVERE, "An exception was thrown opening the store", exception);
      updateMessage(exception.getMessage());
      throw exception;
    }
  }

  @Override
  protected final void failed() {
    logger.severe("Loading store Failed");
  }

  @Override
  protected final void cancelled() {
    logger.severe("Loading store Failed");
  }

  private void checkExportDatabase() throws IOException {

    final Path dbPath = Helpers.expandPath(this.path);
    updateProgress(1, MAX_STEPS);

    updateMessage(resources.getString("workLocation"));
    dbWorkingPath = Files.createTempFile(PLUES, EXTENSION);
    updateProgress(2, MAX_STEPS);


    updateMessage(resources.getString("copy"));
    // create a copy of the database to work on
    try {
      Files.copy(dbPath,
          dbWorkingPath,
          java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    } catch (final IOException exception) {
      updateMessage(exception.getMessage());
      logger.log(Level.SEVERE, "An exception was thrown copying files", exception);
      throw exception;
    }
    properties.put("tempDBpath", dbWorkingPath);
    updateProgress(3, MAX_STEPS);
  }
}

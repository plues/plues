package de.hhu.stups.plues.tasks;

import de.hhu.stups.plues.Helpers;
import de.hhu.stups.plues.data.IncompatibleSchemaError;
import de.hhu.stups.plues.data.SqliteStore;
import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.StoreException;

import javafx.concurrent.Task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StoreLoaderTask extends Task<Store> {
  private final Logger logger = Logger.getLogger(getClass().getSimpleName());

  private static final long MAX_STEPS = 3;
  private static final String PLUES = "plues";
  private static final String EXTENSION = ".sqlite3";

  private Path dbWorkingPath;
  private final String path;

  public StoreLoaderTask(final String storePath) {
    this.path = storePath;
    updateTitle("Opening Database"); // TODO i18n
  }

  @Override
  protected final Store call() throws Exception {
    checkExportDatabase();
    try {
      return new SqliteStore(dbWorkingPath.toString());
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

    updateMessage("Creating a work location"); //TODO: i18n
    dbWorkingPath = Files.createTempFile(PLUES, EXTENSION);
    updateProgress(2, MAX_STEPS);


    updateMessage("Copying files to work location"); // TODO: i18n
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
    updateProgress(3, MAX_STEPS);
  }
}

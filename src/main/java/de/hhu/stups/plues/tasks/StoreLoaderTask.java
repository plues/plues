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

public class StoreLoaderTask extends Task<Store> {
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
        final SqliteStore s = new SqliteStore();
        try {
            s.init(dbWorkingPath.toString());
        } catch (IncompatibleSchemaError|StoreException i) {
            i.printStackTrace();
            updateMessage(i.getMessage());
            s.close();
            throw(i);
        }
        return s;
    }

    @Override
    protected final void failed() {
        System.err.println("Loading store Failed");
    }

    @Override
    protected final void cancelled() {
        System.err.println("Loading store Failed");
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
        } catch (final IOException e) {
            updateMessage(e.getMessage());
            e.printStackTrace();
            throw e;
        }
        updateProgress(3, MAX_STEPS);
    }
}

package de.hhu.stups.plues.tasks;

import com.google.inject.Inject;
import de.hhu.stups.plues.Helpers;
import de.hhu.stups.plues.data.IncompatibleSchemaError;
import de.hhu.stups.plues.data.Store;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class StoreLoaderTask extends Task<Store> {
    private static final long MAX_STEPS = 3;
    private static final String PLUES = "plues";
    private static final String EXTENSION = ".sqlite3";
    private static Path dbPath;
    private static Path dbWorkingPath;
    private final String path;

    public StoreLoaderTask(String path) {
        this.path = path;
        updateTitle("Opening Database"); // TODO i18n
    }

    @Override
    protected Store call() throws Exception {
        checkExportDatabase();
        Store s = new Store();
        try {
            s.init(dbWorkingPath.toString());
        } catch (IncompatibleSchemaError i) {
            i.printStackTrace();
            updateMessage(i.getMessage());
        }
        return s;
    }

    @Override
    protected void failed() {
        System.out.println("Loading store Failed");
    }

    private void checkExportDatabase() throws Exception {

        dbPath = Helpers.expandPath((String) this.path);
        updateProgress(1, MAX_STEPS);

        updateMessage("Creating a work location"); //TODO: i18n
        dbWorkingPath = Files.createTempFile(PLUES, EXTENSION);
        updateProgress(2, MAX_STEPS);


        updateMessage("Copying files to work location"); // TODO: i18n
        // create a copy of the database to work on
        try {
            Files.copy(dbPath, dbWorkingPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            updateMessage(e.getMessage());
            e.printStackTrace();
            throw e;
        }
        updateProgress(3, MAX_STEPS);
    }
}

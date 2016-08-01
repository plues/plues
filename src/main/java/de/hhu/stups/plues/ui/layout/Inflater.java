package de.hhu.stups.plues.ui.layout;

import com.google.inject.Inject;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.net.URL;

/**
 * Created by Tobias Witt on 01.08.16.
 */
public class Inflater {

    private final FXMLLoader loader;

    @Inject
    public Inflater(FXMLLoader loader) {
        this.loader = loader;
    }

    public Parent inflate(String name) {
        // set location explicitly to ensure using the injected fxml loader
        loader.setLocation(getClass().getResource("/fxml/" + name));

        try {
            return loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO: kill app!
        return null;
    }
}

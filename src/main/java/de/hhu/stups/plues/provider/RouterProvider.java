package de.hhu.stups.plues.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import de.hhu.stups.plues.routes.IndexRoute;
import de.hhu.stups.plues.routes.Route;
import de.hhu.stups.plues.ui.Router;
import de.hhu.stups.plues.ui.layout.Inflater;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Tobias Witt on 01.08.16.
 */
public class RouterProvider implements Provider<Router> {
    private Router cache;

    private final Inflater inflater;
    private final Stage stage;

    @Inject
    public RouterProvider(Inflater inflater, Stage stage) {
        this.inflater = inflater;
        this.stage = stage;
    }

    @Override
    public Router get() {
        if (cache == null) {
            cache = new Router();

            cache.put("index", new IndexRoute(inflater, stage));
        }

        return cache;
    }
}

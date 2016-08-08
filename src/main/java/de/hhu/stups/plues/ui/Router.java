package de.hhu.stups.plues.ui;

import com.google.inject.Singleton;
import de.hhu.stups.plues.routes.Route;

import java.util.HashMap;

/**
 * Created by Tobias Witt on 01.08.16.
 */
@Singleton
public class Router extends HashMap<String, Route> {
    public void transitionTo(String routeName) {
        this.get(routeName).transition();
    }
}

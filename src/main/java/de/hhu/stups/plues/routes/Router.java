package de.hhu.stups.plues.routes;

import com.google.inject.Singleton;

import de.hhu.stups.plues.data.entities.Course;

import java.util.HashMap;

/**
 * Allows transitions from scene to scene via a {@link Route}.
 */
@Singleton
public class Router extends HashMap<String, Route> {
  public void transitionTo(String routeName, Course... courses) {
    this.get(routeName).transition(courses);
  }
}

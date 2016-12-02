package de.hhu.stups.plues.routes;

import de.hhu.stups.plues.data.entities.Course;

/**
 * Interface for defining a transition between {@link javafx.scene.Scene}.
 */
@FunctionalInterface
interface Route {
  void transition(Course... courseNames);
}

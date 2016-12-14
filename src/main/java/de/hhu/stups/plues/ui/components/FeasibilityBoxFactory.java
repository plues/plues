package de.hhu.stups.plues.ui.components;

import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.data.entities.Course;
import javafx.scene.layout.VBox;

import java.util.Set;

@FunctionalInterface
public interface FeasibilityBoxFactory {
  FeasibilityBox create(@Assisted("major") Course major, @Assisted("minor") Course minor,
                        VBox parent);
}

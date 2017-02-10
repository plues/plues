package de.hhu.stups.plues.ui.components;

import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.data.entities.Course;

import javafx.scene.control.ListView;

@FunctionalInterface
public interface FeasibilityBoxFactory {
  FeasibilityBox create(@Assisted("major") Course major, @Assisted("minor") Course minor,
                        ListView<FeasibilityBox> parent);
}

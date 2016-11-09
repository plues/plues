package de.hhu.stups.plues.ui.components;

import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.data.entities.Course;

import javafx.scene.layout.VBox;

@FunctionalInterface
public interface ResultBoxFactory {
  ResultBox create(@Assisted("major") Course major, @Assisted("minor") Course minor,
                   @Assisted("parent") VBox parent);
}
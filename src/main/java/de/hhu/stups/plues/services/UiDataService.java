package de.hhu.stups.plues.services;

import com.google.inject.Singleton;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.ObservableList;

/**
 * Stupid data container that can be injected and used by all UI components to store global data.
 */

@Singleton
public class UiDataService {
  private ListProperty<Integer> conflictMarkedSessionsProperty = new SimpleListProperty<>();

  public ObservableList<Integer> getConflictMarkedSessions() {
    return conflictMarkedSessionsProperty.get();
  }

  public ListProperty<Integer> conflictMarkedSessionsProperty() {
    return conflictMarkedSessionsProperty;
  }

  public void setConflictMarkedSessions(ObservableList<Integer> conflictMarkedSessions) {
    this.conflictMarkedSessionsProperty.set(conflictMarkedSessions);
  }
}

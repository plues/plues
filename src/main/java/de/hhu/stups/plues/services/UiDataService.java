package de.hhu.stups.plues.services;

import com.google.inject.Singleton;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

/**
 * Stupid data container that can be injected and used by all UI components to store global data.
 */

@Singleton
public class UiDataService {
  private ListProperty<Integer> conflictMarkedSessionsProperty = new SimpleListProperty<>();
  private StringProperty sessionDisplayFormatProperty = new SimpleStringProperty();

  public String getSessionDisplayFormatProperty() {
    return sessionDisplayFormatProperty.get();
  }

  public StringProperty sessionDisplayFormatProperty() {
    return sessionDisplayFormatProperty;
  }

  public void setSessionDisplayFormatProperty(String prefSessionDisplayFormatProperty) {
    this.sessionDisplayFormatProperty.set(prefSessionDisplayFormatProperty);
  }

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

package de.hhu.stups.plues.ui.components.timetable;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

@FunctionalInterface
public interface SessionListViewFactory {
  @Inject
  SessionListView create(@Assisted final SessionFacade.Slot slotl);
}

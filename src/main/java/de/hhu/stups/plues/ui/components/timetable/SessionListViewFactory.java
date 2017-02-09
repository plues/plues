package de.hhu.stups.plues.ui.components.timetable;

import com.google.inject.assistedinject.Assisted;

public interface SessionListViewFactory {
  SessionListView create(@Assisted final SessionFacade.Slot slotl);
}

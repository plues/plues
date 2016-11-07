package de.hhu.stups.plues.ui.components.timetable;

import com.google.inject.assistedinject.Assisted;

import de.hhu.stups.plues.data.sessions.SessionFacade;

public interface SessionListViewFactory {
  SessionListView create(@Assisted final SessionFacade.Slot slotl);
}

package de.hhu.stups.plues.ui.components.timetable;

import de.hhu.stups.plues.data.sessions.SessionFacade;

import javafx.scene.control.ListCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;

class SessionCell extends ListCell<SessionFacade> {

  SessionCell() {
    super();

    setOnDragDetected(this::dragItem);
  }

  private void dragItem(MouseEvent event) {
    if (getItem() == null) {
      return;
    }

    Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
    ClipboardContent content = new ClipboardContent();
    content.putString(String.valueOf(getItem().getSession().getId()));
    dragboard.setContent(content);
    event.consume();
  }

  @Override
  protected void updateItem(SessionFacade session, boolean empty) {
    super.updateItem(session, empty);

    setText(empty || session == null ? null : session.toString());
  }
}

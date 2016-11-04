package de.hhu.stups.plues.ui.components.timetable;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.data.sessions.SessionFacade;

import javafx.scene.control.ListCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;

import org.controlsfx.control.PopOver;

class SessionCell extends ListCell<SessionFacade> {

  private final Provider<DetailView> provider;
  private SessionFacade.Slot slot;

  @Inject
  SessionCell(final Provider<DetailView> detailViewProvider) {
    super();

    this.provider = detailViewProvider;

    setOnDragDetected(this::dragItem);
    setOnMouseClicked(this::clickItem);
  }

  @SuppressWarnings("unused")
  private void dragItem(final MouseEvent event) {
    if (getItem() == null) {
      return;
    }

    final Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
    final ClipboardContent content = new ClipboardContent();
    content.putString(String.valueOf(getItem().getSession().getId()));
    dragboard.setContent(content);
    event.consume();
  }

  @SuppressWarnings("unused")
  private void clickItem(final MouseEvent event) {
    final Session session = getItem().getSession();

    final DetailView view = provider.get();
    view.setContent(session, slot);

    final PopOver pop = new PopOver(view);
    pop.setPrefHeight(400);
    pop.setPrefWidth(400);
    pop.setTitle("Session Detail");
    pop.show(this); // TODO weitere Parameter zur Positionierung erforderlich aber nicht einheitlich
  }

  @Override
  protected void updateItem(final SessionFacade session, final boolean empty) {
    super.updateItem(session, empty);

    setText(empty || session == null ? null : session.toString());
  }

  public void setSlot(final SessionFacade.Slot slot) {
    this.slot = slot;
  }
}

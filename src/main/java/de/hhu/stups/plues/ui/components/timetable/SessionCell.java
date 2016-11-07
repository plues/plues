package de.hhu.stups.plues.ui.components.timetable;

import com.google.inject.Inject;
import com.google.inject.Provider;

import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.data.sessions.SessionFacade;

import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.stage.Stage;

class SessionCell extends ListCell<SessionFacade> {

  private final Provider<DetailView> provider;
  private SessionFacade.Slot slot;

  @Inject
  SessionCell(final Provider<DetailView> detailViewProvider) {
    super();

    this.provider = detailViewProvider;

    setOnDragDetected(this::dragItem);
    setOnMousePressed(this::clickItem);
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
    if (getItem() == null || event.getClickCount() < 2) {
      return;
    }
    final Session session = getItem().getSession();

    final DetailView detailView = provider.get();
    detailView.setContent(session, slot);

    final Stage stage = new Stage();
    stage.setScene(new Scene(detailView));
    stage.show();
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

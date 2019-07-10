package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.ui.TooltipAllocator;
import de.hhu.stups.plues.ui.layout.Inflater;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

public class ControllerHeader extends HBox {

  @FXML
  @SuppressWarnings("unused")
  private Label lbControllerTitle;
  @FXML
  @SuppressWarnings("unused")
  private Label lbControllerInfo;
  @FXML
  @SuppressWarnings("unused")
  private Tooltip controllerInfoTooltip;

  @Inject
  public ControllerHeader(final Inflater inflater) {
    inflater.inflate("components/ControllerHeader", this, this);
  }

  @FXML
  public void initialize() {
    TooltipAllocator.showTooltipOnEnter(lbControllerInfo, controllerInfoTooltip,
        new SimpleBooleanProperty(false));
    lbControllerInfo.graphicProperty().bind(Bindings.createObjectBinding(() ->
        FontAwesomeIconFactory.get().createIcon(FontAwesomeIcon.INFO_CIRCLE, "12")));
  }

  public void setTitle(final String title) {
    lbControllerTitle.setText(title);
  }

  public void setInfoText(final String infoText) {
    controllerInfoTooltip.setText(infoText);
  }
}

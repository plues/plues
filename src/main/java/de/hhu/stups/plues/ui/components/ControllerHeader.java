package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.ui.TooltipAllocator;
import de.hhu.stups.plues.ui.layout.Inflater;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.utils.FontAwesomeIconFactory;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.ResourceBundle;

public class ControllerHeader extends HBox implements Initializable {

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

  @Override
  public void initialize(final URL location, final ResourceBundle resources) {
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

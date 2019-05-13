package de.hhu.stups.plues.ui.components;

import com.google.inject.Inject;

import de.hhu.stups.plues.ui.layout.Inflater;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

import java.util.ResourceBundle;

/**
 * A simple component to select the {@link UnitDisplayFormat} to be used for the PDF timetable
 * generation. This is either the title or the key of an abstract unit.
 */
public class UnitDisplayFormatSelection extends GridPane {

  @FXML
  private ResourceBundle resources;

  private ObjectProperty<UnitDisplayFormat> selectedDisplayFormatProperty;

  @FXML
  @SuppressWarnings("unused")
  private ComboBox<UnitDisplayFormat> cbPdfUnitDisplayFormat;
  @FXML
  @SuppressWarnings("unused")
  private ColumnConstraints columnConstraints;

  /**
   * Initialize property and inflate the layout.
   */
  @Inject
  public UnitDisplayFormatSelection(final Inflater inflater) {
    selectedDisplayFormatProperty = new SimpleObjectProperty<>();
    inflater.inflate("components/UnitDisplayFormatSelection", this, this,
        "unitDisplayFormatSelection");
  }

  @FXML
  public void initialize() {
    selectedDisplayFormatProperty
        .bind(cbPdfUnitDisplayFormat.getSelectionModel().selectedItemProperty());
    cbPdfUnitDisplayFormat.setConverter(new StringConverter<UnitDisplayFormat>() {
      @Override
      public String toString(final UnitDisplayFormat unitDisplayFormat) {
        return resources.getString(unitDisplayFormat.toString());
      }

      @Override
      public UnitDisplayFormat fromString(final String string) {
        return null;
      }
    });
    cbPdfUnitDisplayFormat.getItems().addAll(UnitDisplayFormat.TITLE, UnitDisplayFormat.ID);
    cbPdfUnitDisplayFormat.getSelectionModel().selectFirst();
  }

  /**
   * Set the percent width of this component according to the node it is placed in.
   */
  public void setPercentWidth(final double percentWidth) {
    columnConstraints.setPercentWidth(percentWidth);
  }

  public ObjectProperty<UnitDisplayFormat> selectedDisplayFormatProperty() {
    return selectedDisplayFormatProperty;
  }
}

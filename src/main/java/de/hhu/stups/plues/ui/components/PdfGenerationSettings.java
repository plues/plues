package de.hhu.stups.plues.ui.components;

import de.hhu.stups.plues.studienplaene.ColorScheme;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * A wrapper class for settings used to generate a PDF timetable. The current settings are the
 * selected {@link de.hhu.stups.plues.studienplaene.ColorScheme} and the {@link UnitDisplayFormat}.
 */
public class PdfGenerationSettings {

  private final ObjectProperty<ColorScheme> colorSchemeProperty;
  private final ObjectProperty<UnitDisplayFormat> unitDisplayFormatProperty;

  public PdfGenerationSettings(final ColorScheme colorScheme,
                               final UnitDisplayFormat unitDisplayFormat) {
    colorSchemeProperty = new SimpleObjectProperty<>(colorScheme);
    unitDisplayFormatProperty = new SimpleObjectProperty<>(unitDisplayFormat);
  }

  public ObjectProperty<ColorScheme> colorSchemeProperty() {
    return colorSchemeProperty;
  }

  public ObjectProperty<UnitDisplayFormat> unitDisplayFormatProperty() {
    return unitDisplayFormatProperty;
  }
}

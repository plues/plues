package de.hhu.stups.plues.studienplaene;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Group;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.ui.components.PdfGenerationSettings;
import de.hhu.stups.plues.ui.components.UnitDisplayFormat;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class DataStoreWrapper {

  @SuppressWarnings("unchecked")
  private final Map<String, String>[] semesters = new Map[6];
  private final Map<String, String> colorMap;
  private final Map<String, String> fonts;
  private final ColorPalette colors;
  private final UnitDisplayFormat unitDisplayFormat;

  DataStoreWrapper(final PdfGenerationSettings pdfGenerationSettings, final DataPreparatory data) {
    for (int k = 0; k < semesters.length; k++) {
      semesters[k] = new HashMap<>();
    }
    final ColorScheme colorScheme = pdfGenerationSettings.colorSchemeProperty().get();
    colors = colorScheme.isGrayscale() ? new Grayscale() : new Colored(colorScheme.getColors());
    colorMap = new HashMap<>();
    unitDisplayFormat = pdfGenerationSettings.unitDisplayFormatProperty().get();
    fonts = new HashMap<>();
    createData(data);
  }

  private void createData(final DataPreparatory data) {
    for (final Map.Entry<AbstractUnit, Integer> choice : data.getUnitSemester().entrySet()) {
      final AbstractUnit abstractUnit = choice.getKey();
      final Module module = data.getUnitModule().get(abstractUnit);
      final Integer semester = choice.getValue();
      final Group group = data.getUnitGroup().get(abstractUnit);

      for (final Session session : group.getSessions()) {
        createSessionData(abstractUnit, module, semester, group, session);
      }
    }
  }

  private void createSessionData(final AbstractUnit abstractUnit, final Module module,
                                 final Integer semester, final Group group, final Session session) {

    final boolean isSpecial = isSpecial(session) || isSpecial(group);
    final int semesterIndex = semester - 1;
    //
    final String key = session.getDay() + session.getTime();
    final String content = semesters[semesterIndex].get(key);
    //
    final StringBuilder title = getTitleBuilder(abstractUnit, group, session);
    //
    if (content != null && isSpecial) {
      handleSpecialContent(key, title.toString(), semesters[semesterIndex], content);
    } else {
      handleCommonContent(module.getTitle(), key, title, semesters[semesterIndex]);
    }
  }

  private StringBuilder getTitleBuilder(final AbstractUnit abstractUnit, final Group group,
                                        final Session session) {

    final StringBuilder title = new StringBuilder(getTitleOrKeyForDisplayFormat(abstractUnit));

    title.append(getTitlePart(session));
    title.append(getTitlePart(group));

    return title;
  }

  private void handleCommonContent(final String moduleName, final String key,
                                   final StringBuilder title, final Map<String, String> semester) {

    final Color c = colors.nextColor();

    title.append(';').append(moduleName);
    semester.put(key, title.toString());

    colorMap.put(moduleName, getColorString(c));
    fonts.put(moduleName, getFontColor(c));
  }

  private void handleSpecialContent(final String key, final String title,
                                    final Map<String, String> semester, final String content) {

    final String[] values = content.split(";");
    final String newContent = String.format("%s;%s;%s", values[0], title, values[1]);
    //
    semester.remove(key);
    semester.put(key, newContent);
    //
    final Color c = colors.nextColor();
    colorMap.put(values[1], getColorString(c));
    fonts.put(values[1], getFontColor(c));
  }

  private String getTitlePart(final Group group) {
    final int halfSemester = group.getHalfSemester();
    switch (halfSemester) {
      case 1:
        return " (f)";
      case 2:
        return " (s)";
      default:
        return "";
    }
  }

  private String getTitlePart(final Session session) {
    final Integer rhythm = session.getRhythm();
    switch (rhythm) {
      case 0:
        return "";
      case 1:
        return " (A)";
      case 2:
        return " (B)";
      case 3:
        return " (b)";
      default:
        throw new AssertionError("Unsupported rhythm");
    }
  }

  private boolean isSpecial(final Group group) {
    return group.getHalfSemester() > 0;
  }

  private boolean isSpecial(final Session session) {
    final Integer rhythm = session.getRhythm();
    return rhythm != 0;
  }

  private String getFontColor(final Color backgroundColor) {
    final double brightness
      = 1 - (0.299 * backgroundColor.getRed()
      + 0.587 * backgroundColor.getGreen()
      + 0.114 * backgroundColor.getBlue()) / 255;

    return (brightness < 0.5) ? "black" : "white";
  }

  private String getColorString(final Color color) {
    String red = Integer.toHexString(color.getRed());
    if (red.length() == 1) {
      red = "0" + red;
    }

    String green = Integer.toHexString(color.getGreen());
    if (green.length() == 1) {
      green = "0" + green;
    }

    String blue = Integer.toHexString(color.getBlue());
    if (blue.length() == 1) {
      blue = "0" + blue;
    }

    return "#" + red + green + blue;
  }

  final Map<String, String>[] getSemesters() {
    return Arrays.copyOf(semesters, semesters.length);
  }

  final Map<String, String> getColorMap() {
    return colorMap;
  }

  final Map<String, String> getFonts() {
    return fonts;
  }

  private String getTitleOrKeyForDisplayFormat(final AbstractUnit abstractUnit) {
    final String title = abstractUnit.getTitle();
    if (unitDisplayFormat.isTitle()) {
      return title;
    } else {
      final String key = abstractUnit.getKey();
      // if an abstract unit has no key we display its title
      return key != null ? key : title;
    }
  }
}

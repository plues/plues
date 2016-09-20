package de.hhu.stups.plues.studienplaene;

import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Group;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.Session;

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

  DataStoreWrapper(final boolean cc, final DataPreparatory data) {
    for (int k = 0; k < semesters.length; k++) {
      semesters[k] = new HashMap<>();
    }
    if (cc) {
      colors = new Colored();
    } else {
      colors = new Grayscale();
    }

    colorMap = new HashMap<>();
    fonts = new HashMap<>();

    createData(data);
  }

  private void createData(final DataPreparatory data) {
    Integer semester;
    Group group; // Need to be checked (getSessions())
    AbstractUnit abstractUnit;
    Module module;

    for (final Map.Entry<AbstractUnit, Integer> choice : data.getUnitSemester().entrySet()) {
      abstractUnit = choice.getKey();
      module = data.getUnitModule().get(abstractUnit);
      semester = choice.getValue();
      group = data.getUnitGroup().get(abstractUnit);

      for (final Session s : group.getSessions()) {
        final String key = "" + s.getDay() + s.getTime();

        final StringBuilder title = new StringBuilder(abstractUnit.getTitle());
        boolean isSpecial;

        // check for additional info (rhythm / half_semester)
        final Integer i = s.getRhythm();
        switch (i) {
          case 0:
            isSpecial = false;
            break;
          case 1:
            title.append(" (A)");
            isSpecial = true;
            break;
          case 2:
            title.append(" (B)");
            isSpecial = true;
            break;
          case 3:
            title.append(" (b)");
            isSpecial = true;
            break;
          default:
            throw new RuntimeException("Unsopported rhythm");
        }

        final int i1 = group.getHalfSemester();
        if (i1 == 1) {
          title.append(" (f)");
          isSpecial = true;
        } else if (i1 == 2) {
          title.append(" (s)");
          isSpecial = true;
        }

        // check for content
        final String content = semesters[semester - 1].get(key);
        if (content != null && isSpecial) {
          semesters[semester - 1].remove(key);
          final String[] values = content.split(";");
          final String newContent = values[0] + " / "
              + title.toString() + ";" + values[1];
          semesters[semester - 1].put(key, newContent);
          setColorToBlack(values[1]);
        } else {
          final String moduleName = module.getName();
          title.append(';').append(moduleName);
          semesters[semester - 1].put(key, title.toString());
          colorMap.put(moduleName, getColorString(moduleName));
        }

      }
    }
  }

  private void setColorToBlack(final String module) {
    fonts.remove(module);
    fonts.put(module, "white");
    colorMap.remove(module);
    colorMap.put(module, "#000000");
  }

  private String getColorString(final String module) {
    final Color c = colors.nextColor();
    String red = Integer.toHexString(c.getRed());
    if (red.length() == 1) {
      red = "0" + red;
    }
    String green = Integer.toHexString(c.getGreen());
    if (green.length() == 1) {
      green = "0" + green;
    }
    String blue = Integer.toHexString(c.getBlue());
    if (blue.length() == 1) {
      blue = "0" + blue;
    }
    final double brightness = 1 - (0.299 * c.getRed() + 0.587 * c.getGreen()
        + 0.114 * c.getBlue()) / 255;
    if (brightness < 0.5) {
      fonts.put(module, "black");
    } else {
      fonts.put(module, "white");
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
}

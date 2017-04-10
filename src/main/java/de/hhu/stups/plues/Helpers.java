package de.hhu.stups.plues;

import de.hhu.stups.plues.data.entities.Course;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Helpers {

  public static final Map<String, String> timeIntervalMap;

  static {
    final LinkedHashMap<String, String> tempMap = new LinkedHashMap<>(6);
    tempMap.put("1", "08:30-10:00");
    tempMap.put("2", "10:30-12:00");
    tempMap.put("3", "12:30-14:00");
    tempMap.put("4", "14:30-16:00");
    tempMap.put("5", "16:30-18:00");
    tempMap.put("6", "18:30-20:00");
    timeIntervalMap = Collections.unmodifiableMap(tempMap);
  }

  public static final Map<Integer, String> timeMap;

  static {
    final LinkedHashMap<Integer, String> tempMap = new LinkedHashMap<>(6);
    tempMap.put(1, "08:30");
    tempMap.put(2, "10:30");
    tempMap.put(3, "12:30");
    tempMap.put(4, "14:30");
    tempMap.put(5, "16:30");
    tempMap.put(6, "18:30");
    tempMap.put(7, "20:30");
    timeMap = Collections.unmodifiableMap(tempMap);
  }

  public static final Map<DayOfWeek, String> shortDayOfWeekMap;

  static {
    final EnumMap<DayOfWeek, String> tempMap = new EnumMap<>(DayOfWeek.class);
    tempMap.put(DayOfWeek.MONDAY, "mon");
    tempMap.put(DayOfWeek.TUESDAY, "tue");
    tempMap.put(DayOfWeek.WEDNESDAY, "wed");
    tempMap.put(DayOfWeek.THURSDAY, "thu");
    tempMap.put(DayOfWeek.FRIDAY, "fri");
    tempMap.put(DayOfWeek.SATURDAY, "sat");
    tempMap.put(DayOfWeek.SUNDAY, "sun");
    shortDayOfWeekMap = Collections.unmodifiableMap(tempMap);
  }


  private Helpers() {
  }

  /**
   * Expand a path (gives as string) to an absolute path. If the path starts with ~ it is replaced
   * with the current user's home directory.
   *
   * @param base String
   * @return Path absulute path representation of the argument
   */
  public static Path expandPath(final String base) {
    // handle ~ in paths
    final String basePath;
    if (base.startsWith("~" + File.separator)) {
      basePath = System.getProperty("user.home") + base.substring(1);
    } else {
      basePath = base;
    }
    return FileSystems.getDefault().getPath(basePath).toAbsolutePath();
  }

  /**
   * Check if two courses are equal or both are null.
   */
  public static boolean equalCoursesOrNull(final Course course1, final Course course2) {
    return (course1 == null && course2 == null) || (course1 != null && course1.equals(course2));
  }
}

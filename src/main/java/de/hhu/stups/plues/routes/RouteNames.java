package de.hhu.stups.plues.routes;

public enum RouteNames {
  ABOUT_WINDOW("aboutWindow"),
  ABSTRACT_UNIT_DETAIL_VIEW("abstractUnitDetailView"),
  CHANGELOG("changelog"),
  COURSE("courseDetailView"),
  INDEX("index"),
  MODULE_DETAIL_VIEW("moduleDetailView"),
  PARTIAL_TIMETABLES("partialTimetables"),
  PDF_TIMETABLES("pdfTimetables"),
  REPORTS("Reports"),
  SESSION_DETAIL_VIEW("sessionDetailView"),
  TIMETABLE("timetable"),
  UNIT_DETAIL_VIEW("unitDetailView"),
  UNSAT_CORE("unsatCore");

  private final String routeName;

  RouteNames(final String routeName) {
    this.routeName = routeName;
  }

  public String getRouteName() {
    return routeName;
  }
}

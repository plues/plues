package de.hhu.stups.plues.routes;

public enum RouteNames {
  INDEX("index"),
  MODULE_DETAIL_VIEW("moduleDetailView"),
  SESSION_DETAIL_VIEW("sessionDetailView"),
  ABSTRACT_UNIT_DETAIL_VIEW("abstractUnitDetailView"),
  UNIT_DETAIL_VIEW("unitDetailView");

  private final String routeName;

  RouteNames(final String routeName) {
    this.routeName = routeName;
  }

  public String getRouteName() {
    return routeName;
  }
}

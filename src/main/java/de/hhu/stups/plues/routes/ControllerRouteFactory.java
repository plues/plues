package de.hhu.stups.plues.routes;

import com.google.inject.assistedinject.Assisted;

@FunctionalInterface
public interface ControllerRouteFactory {
  ControllerRoute create(@Assisted String tabId);
}

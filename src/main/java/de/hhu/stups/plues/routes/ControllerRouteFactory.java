package de.hhu.stups.plues.routes;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

@FunctionalInterface
public interface ControllerRouteFactory {
  @Inject
  ControllerRoute create(@Assisted String tabId);
}

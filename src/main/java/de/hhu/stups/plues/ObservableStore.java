package de.hhu.stups.plues;

import de.hhu.stups.plues.data.Store;
import de.hhu.stups.plues.data.StoreException;
import de.hhu.stups.plues.data.entities.AbstractUnit;
import de.hhu.stups.plues.data.entities.Course;
import de.hhu.stups.plues.data.entities.Group;
import de.hhu.stups.plues.data.entities.Level;
import de.hhu.stups.plues.data.entities.Log;
import de.hhu.stups.plues.data.entities.Module;
import de.hhu.stups.plues.data.entities.ModuleAbstractUnitSemester;
import de.hhu.stups.plues.data.entities.ModuleAbstractUnitType;
import de.hhu.stups.plues.data.entities.ModuleLevel;
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.data.entities.Unit;
import org.reactfx.EventSource;

import java.util.List;

public class ObservableStore implements Store {

  private final Store store;

  public EventSource<String> getChanges() {
    return changes;
  }

  private final EventSource<String> changes;

  public ObservableStore(final Store store) {
    this.changes = new EventSource<>();
    this.store = store;
  }

  @Override
  public void init() throws StoreException {
    store.init();
  }

  @Override
  public void init(final String dbpath) throws StoreException {
    store.init(dbpath);
  }

  /**
   * Undo the last move operation.
   */
  public void undoLastMoveOperation(final Log lastLog) {
    if (lastLog == null) {
      return;
    }
    store.moveSession(lastLog.getSession().getId(), lastLog.getSrcDay(),
        lastLog.getSrcTime());
    // remove the last two log entries since we have the undo log and the log itself that we
    // want to undo
    removeLastLogEntry();
    removeLastLogEntry();
    //
    changes.push("removed");
  }

  /**
   * Redo the last move operation.
   */
  public void redoLastMoveOperation(final Log lastLog) {
    if (lastLog == null) {
      return;
    }
    store.moveSession(lastLog.getSession().getId(), lastLog.getTargetDay(),
        lastLog.getTargetTime());
    changes.push("");
  }

  @Override
  public void moveSession(final int sessionId, final String targetDay, final Integer targetTime) {
    store.moveSession(sessionId, targetDay, targetTime);
    changes.push("");
  }

  @Override
  public void close() {
    store.close();
  }

  @Override
  public void clear() {
    store.clear();
  }

  @Override
  public List getInfo() {
    return store.getInfo();
  }

  @Override
  public List<AbstractUnit> getAbstractUnits() {
    return store.getAbstractUnits();
  }

  @Override
  public List<AbstractUnit> getAbstractUnitsWithoutUnits() {
    return store.getAbstractUnitsWithoutUnits();
  }

  @Override
  public List<Course> getCourses() {
    return store.getCourses();
  }

  @Override
  public Course getCourseByKey(final String key) {
    return store.getCourseByKey(key);
  }

  @Override
  public List<ModuleLevel> getModuleLevels() {
    return store.getModuleLevels();
  }

  @Override
  public List<Group> getGroups() {
    return store.getGroups();
  }

  @Override
  public List<Level> getLevels() {
    return store.getLevels();
  }

  @Override
  public List<Module> getModules() {
    return store.getModules();
  }

  @Override
  public List<ModuleAbstractUnitSemester> getModuleAbstractUnitSemester() {
    return store.getModuleAbstractUnitSemester();
  }

  @Override
  public List<ModuleAbstractUnitType> getModuleAbstractUnitType() {
    return store.getModuleAbstractUnitType();
  }

  @Override
  public List<Session> getSessions() {
    return store.getSessions();
  }

  @Override
  public List<Unit> getUnits() {
    return store.getUnits();
  }

  @Override
  public Unit getUnitById(final Integer uid) {
    return store.getUnitById(uid);
  }

  @Override
  public AbstractUnit getAbstractUnitById(final Integer key) {
    return store.getAbstractUnitById(key);
  }

  @Override
  public Group getGroupById(final Integer integer) {
    return store.getGroupById(integer);
  }

  @Override
  public String getInfoByKey(final String key) {
    return store.getInfoByKey(key);
  }

  @Override
  public Module getModuleById(final Integer mid) {
    return store.getModuleById(mid);
  }

  @Override
  public List<Log> getLogEntries() {
    return store.getLogEntries();
  }

  @Override
  public Log getLastLogEntry() {
    return store.getLastLogEntry();
  }

  @Override
  public void removeLastLogEntry() {
    store.removeLastLogEntry();
  }

  @Override
  public Session getSessionById(final int id) {
    return store.getSessionById(id);
  }

  @Override
  public List<Course> getMajors() {
    return store.getMajors();
  }

  @Override
  public List<Course> getMinors() {
    return store.getMinors();
  }
}

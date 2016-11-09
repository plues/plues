package de.hhu.stups.plues;

import de.hhu.stups.plues.data.IncompatibleSchemaError;
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
import de.hhu.stups.plues.data.entities.Session;
import de.hhu.stups.plues.data.entities.Unit;
import de.hhu.stups.plues.data.sessions.SessionFacade;

import java.util.List;
import java.util.Observable;

public class ObservableStore extends Observable implements Store {

  private final Store store;

  public ObservableStore(final Store store) {
    this.store = store;
  }

  @Override
  public void init() throws IncompatibleSchemaError, StoreException {
    store.init();
  }

  @Override
  public void init(String dbpath) throws IncompatibleSchemaError, StoreException {
    store.init(dbpath);
  }

  @Override
  public void moveSession(SessionFacade sessionFacade, SessionFacade.Slot slot) {
    store.moveSession(sessionFacade, slot);
    setChanged();
    notifyObservers();
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
  public Course getCourseByKey(String key) {
    return store.getCourseByKey(key);
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
  public Unit getUnitById(Integer uid) {
    return store.getUnitById(uid);
  }

  @Override
  public AbstractUnit getAbstractUnitById(Integer key) {
    return store.getAbstractUnitById(key);
  }

  @Override
  public Group getGroupById(Integer integer) {
    return store.getGroupById(integer);
  }

  @Override
  public String getInfoByKey(String key) {
    return store.getInfoByKey(key);
  }

  @Override
  public Module getModuleById(Integer mid) {
    return store.getModuleById(mid);
  }

  @Override
  public List<Log> getLogEntries() {
    return store.getLogEntries();
  }

  @Override
  public Session getSessionById(int id) {
    return store.getSessionById(id);
  }
}

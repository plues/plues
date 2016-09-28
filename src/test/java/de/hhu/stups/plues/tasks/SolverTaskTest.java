package de.hhu.stups.plues.tasks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.hhu.stups.plues.prob.Alternative;
import de.hhu.stups.plues.prob.FeasibilityResult;
import de.hhu.stups.plues.prob.Solver;
import de.hhu.stups.plues.prob.SolverException;
import javafx.application.Platform;
import javafx.collections.ObservableMap;
import javafx.concurrent.Worker;
import javafx.stage.Stage;
import org.junit.Ignore;
import org.junit.Test;
import org.testfx.framework.junit.ApplicationTest;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class SolverTaskTest extends ApplicationTest {

  private static final ExecutorService executor;
  private static final String TITLE = "Title";
  private static final String MESSAGE = "Message";
  private static final ResourceBundle resources = ResourceBundle.getBundle("lang.tasks");

  static {
    final ThreadFactory threadFactory
        = new ThreadFactoryBuilder().setDaemon(true).setNameFormat("SolverLoaderTask-%d").build();
    executor = Executors.newSingleThreadExecutor(threadFactory);
  }

  @Test
  public void testCallableIsSuccessful() throws ExecutionException, InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    final SolverTask<Integer> solverTask
        = new SolverTask<>(TITLE, MESSAGE, new TestSolver(), () -> 1);
    final TaskProperties taskProperties = new TaskProperties();

    Platform.runLater(() -> {
      solverTask.setOnSucceeded(event -> {
        taskProperties.setMessage(solverTask.getMessage());
        taskProperties.setTitle(solverTask.getTitle());
        taskProperties.setDone(solverTask.isDone());
        taskProperties.setValue(solverTask.getValue());
        latch.countDown();
      });
      executor.submit(solverTask);
    });

    final Integer b = solverTask.get();

    // wait until the onSucceeded event handler finishes
    latch.await();

    assertEquals(MESSAGE, taskProperties.getMessage());
    assertEquals(TITLE, taskProperties.getTitle());

    assertTrue(taskProperties.isDone());

    assertEquals(Integer.valueOf(1), taskProperties.getValue());
    assertEquals(Integer.valueOf(1), b);
  }

  @Test
  public void testCallableFails() throws ExecutionException, InterruptedException {
    final Callable<Integer> c = () -> {
      throw new TestException("NO");
    };
    final SolverTask<Integer> solverTask
        = new SolverTask<>(TITLE, MESSAGE, new TestSolver(), c);

    executor.submit(solverTask);

    try {
      solverTask.get();
      fail();
    } catch (final ExecutionException exception) {
      final Throwable cause = exception.getCause();
      assertSame(ExecutionException.class, cause.getClass());

      final Throwable realCause = cause.getCause();
      assertSame(TestException.class, realCause.getClass());
      assertEquals("NO", realCause.getMessage());
    }

    final TaskProperties taskProperties = getTaskProperties(solverTask);

    // wait until the code above ran on the JavaFX thread

    assertEquals(resources.getString("failed"), taskProperties.getMessage());
    assertEquals(TITLE, taskProperties.getTitle());

    assertTrue(taskProperties.isDone());
    assertEquals(taskProperties.getState(), Worker.State.FAILED);
    assertEquals(false, taskProperties.isCancelled());

  }

  @Test
  public void testTaskIsCancelled() throws InterruptedException {
    final Callable<Integer> c = () -> {
      TimeUnit.DAYS.sleep(365);
      throw new TestException("NO");
    };
    final SolverTask<Integer> solverTask
        = new SolverTask<>(TITLE, MESSAGE, new TestSolver(), c);

    Platform.runLater(() -> {
      executor.submit(solverTask);
      solverTask.cancel();
    });
    try {
      solverTask.get();
      fail();
    } catch (final CancellationException cancellationException) {
      final TaskProperties taskProperties = getTaskProperties(solverTask);

      assertEquals(resources.getString("cancelled"), taskProperties.getMessage());
      assertEquals(TITLE, taskProperties.getTitle());

      assertTrue(taskProperties.isDone());
      assertEquals(taskProperties.getState(), Worker.State.CANCELLED);
      assertTrue(taskProperties.isCancelled());
    } catch (InterruptedException | ExecutionException exception) {
      fail();
    }
  }

  @Test
  public void testTaskTimeout() throws InterruptedException {
    final Callable<Integer> c = () -> {
      TimeUnit.DAYS.sleep(365);
      return 1;
    };
    final SolverTask<Integer> solverTask
        = new SolverTask<>(TITLE, MESSAGE, new TestSolver(), c, 3, TimeUnit.SECONDS);
    executor.submit(solverTask);

    try {
      solverTask.get();
      fail();
    } catch (final ExecutionException | InterruptedException exception) {
      exception.printStackTrace();
      fail();
    } catch (final CancellationException cancellationException) {
      final TaskProperties taskProperties = getTaskProperties(solverTask);
      assertEquals(resources.getString("timeout"), taskProperties.getMessage());
      assertEquals(TITLE, taskProperties.getTitle());

      assertTrue(taskProperties.isDone());
      assertEquals(taskProperties.getState(), Worker.State.CANCELLED);
      assertTrue(taskProperties.isCancelled());

    }
  }

  private TaskProperties getTaskProperties(final SolverTask<Integer> solverTask)
      throws InterruptedException {
    final TaskProperties taskProperties = new TaskProperties();
    final CountDownLatch latch = new CountDownLatch(1);
    Platform.runLater(() -> {
      taskProperties.setMessage(solverTask.getMessage());
      taskProperties.setTitle(solverTask.getTitle());
      taskProperties.setDone(solverTask.isDone());
      taskProperties.setValue(solverTask.getValue());
      taskProperties.setCancelled(solverTask.isCancelled());
      taskProperties.setState(solverTask.getState());
      latch.countDown();
    });
    latch.await();
    return taskProperties;
  }

  @Override
  public void start(final Stage stage) throws Exception {
    // required by base class
    // only needed to initialize JavaFX
  }

  private static class TestSolver implements Solver {

    @Override
    public void checkModelVersion(final String expectedVersion) {
      // required by interface
    }

    @Override
    public void interrupt() {
      // required by interface
    }

    @Override
    public Boolean checkFeasibility(final String... courses) {
      return false;
    }

    @Override
    public FeasibilityResult computeFeasibility(final String... courses) {
      return null;
    }

    @Override
    public FeasibilityResult computePartialFeasibility(final List<String> courses,
        final Map<String, List<Integer>> moduleChoice, final List<Integer> abstractUnitChoice) {
      return null;
    }

    @Override
    public List<Integer> unsatCore(final String... courses) {
      return null;
    }

    @Override
    public void move(final String sessionId, final String day, final String slot) {

    }

    @Override
    public Set<String> getImpossibleCourses() {
      return Collections.emptySet();
    }

    @Override
      public List<Alternative> getLocalAlternatives(final int session, final String... courses) {
      return Collections.emptyList();
    }

    @Override
    public String getModelVersion() {
      return null;
    }

    @Override
    public final ObservableMap<String, Boolean> getCourseCombinationResults() {
      return null;
    }
  }

  private static class TaskProperties {

    private String message;
    private String title;
    private Integer value;
    private boolean done;
    private boolean cancelled;
    private Worker.State state;

    private String getMessage() {
      return message;
    }

    void setMessage(final String message) {
      this.message = message;
    }

    private Integer getValue() {
      return value;
    }

    void setValue(final Integer value) {
      this.value = value;
    }

    private boolean isDone() {
      return done;
    }

    void setDone(final boolean done) {
      this.done = done;
    }

    private String getTitle() {
      return title;
    }

    void setTitle(final String title) {
      this.title = title;
    }

    private boolean isCancelled() {
      return cancelled;
    }

    private void setCancelled(final boolean cancelled) {
      this.cancelled = cancelled;
    }

    private Worker.State getState() {
      return state;
    }

    private void setState(final Worker.State state) {
      this.state = state;
    }
  }

  private class TestException extends RuntimeException {
    TestException(final String message) {
      super(message);
    }
  }
}

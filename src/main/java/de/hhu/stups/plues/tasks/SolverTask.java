package de.hhu.stups.plues.tasks;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.hhu.stups.plues.prob.Solver;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javafx.concurrent.Task;

public class SolverTask<T> extends Task<T> {

    private static final ExecutorService EXECUTOR;
      static {
        final ThreadFactory threadFactoryBuilder
          = new ThreadFactoryBuilder().setDaemon(true)
                                      .setNameFormat("solver-task-runner-%d")
                                      .build();
        EXECUTOR = Executors.newSingleThreadExecutor(threadFactoryBuilder);
      }

    private final Callable<T> function;
    private final Solver solver;
    private Future<T> r;
    private final int solverTaskTimeout = 5;    // minutes

    SolverTask(final String title, final String message, final Solver s,
               final Callable<T> func) {
        this(title, s, func);

        updateMessage(message);
    }

    SolverTask(final String title, final Solver s, final Callable<T> func) {
        this.function = func;
        this.solver = s;

        updateTitle(title);
    }

    @Override
    protected T call() throws InterruptedException, ExecutionException {
        updateTitle("Starting Task");
        updateProgress(10, 100);
        r = EXECUTOR.submit(function);

        try {
            r.get(solverTaskTimeout,TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            r.cancel(true);
        }

        int p = 10;
        while(!r.isDone()) {
            p = (p + 5) % 95;
            updateProgress(p, 100);
            if(this.isCancelled()) {
                updateMessage("Task canceled");
                return null;
            }
            if(r.isCancelled()) {
                updateMessage("ProB exited");
                return null;
            }
            TimeUnit.MILLISECONDS.sleep(500);
        }
        updateProgress(100, 100);
        return r.get();
    }

    @Override
    protected void cancelled() {
        super.cancelled();
        r.cancel(true);
        System.out.println("Task cancelled.");
        solver.interrupt();
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        updateMessage("Done!");
        final T i = this.getValue();
        System.out.println("Result: " + i.toString());
    }
}

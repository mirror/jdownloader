package org.jdownloader.captcha.v2;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.appwork.scheduler.DelayedRunnable;
import org.jdownloader.captcha.v2.solverjob.SolverJob;

public class JobRunnable<T> implements Runnable {

    private SolverJob<T>                            job;
    private ChallengeSolver<T>                      solver;
    private boolean                                 canceled;

    public final static ScheduledThreadPoolExecutor TIMINGQUEUE = new ScheduledThreadPoolExecutor(1);

    public JobRunnable(ChallengeSolver<T> challengeSolver, SolverJob<T> job) {
        this.job = job;
        this.solver = challengeSolver;

    }

    public SolverJob<T> getJob() {
        return job;
    }

    @Override
    public void run() {
        if (canceled) return;
        thread = Thread.currentThread();
        if (canceled) return;
        try {
            job.fireBeforeSolveEvent(solver);

            DelayedRunnable timeout = null;
            // final Thread thread = Thread.currentThread();
            if (solver.getTimeout() > 0) {
                timeout = new DelayedRunnable(TIMINGQUEUE, solver.getTimeout()) {
                    @Override
                    public void delayedrun() {
                        System.out.println("Timeout!");
                        if (!job.isDone(solver)) {
                            solver.kill(job);
                            job.fireTimeoutEvent(solver);

                        }
                    }
                };

            }

            try {

                if (timeout != null) timeout.resetAndStart();

                solver.solve(job);
            } catch (Throwable e) {
                getJob().getLogger().log(e);

            } finally {
                if (timeout != null) timeout.stop();
                job.fireAfterSolveEvent(solver);
            }
        } finally {
            thread = null;
        }
    }

    private Thread thread;

    public Thread getThread() {
        return thread;
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }

    public void cancel() {
        this.canceled = true;
        if (thread != null && thread != Thread.currentThread()) {
            thread.interrupt();
        }
    }

}

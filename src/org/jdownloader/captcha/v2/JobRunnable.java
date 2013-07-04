package org.jdownloader.captcha.v2;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import jd.controlling.captcha.SkipException;

import org.appwork.scheduler.DelayedRunnable;
import org.jdownloader.captcha.v2.solverjob.SolverJob;

public class JobRunnable<T> implements Runnable {

    private SolverJob<T>                            job;
    private ChallengeSolver<T>                      solver;
    private boolean                                 canceled;

    public final static ScheduledThreadPoolExecutor TIMINGQUEUE = new ScheduledThreadPoolExecutor(1);
    static {
        TIMINGQUEUE.setKeepAliveTime(10000, TimeUnit.MILLISECONDS);
        TIMINGQUEUE.allowCoreThreadTimeOut(true);
    }

    public JobRunnable(ChallengeSolver<T> challengeSolver, SolverJob<T> job) {
        this.job = job;
        this.solver = challengeSolver;

    }

    public SolverJob<T> getJob() {
        return job;
    }

    @Override
    public void run() {

        try {
            synchronized (this) {
                if (canceled) return;

                getJob().getLogger().info(solver + " RUN!");
                thread = Thread.currentThread();
                thread.setName(solver + "-Thread");
            }
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

            } catch (SkipException e) {

                ChallengeResponseController.getInstance().setSkipRequest(e.getSkipRequest(), solver, job.getChallenge());

            } catch (Throwable e) {
                getJob().getLogger().log(e);

            } finally {
                if (timeout != null) timeout.stop();
                job.fireAfterSolveEvent(solver);
                job.setSolverDone(solver);

            }
        } finally {
            thread = null;
        }
    }

    private Thread thread;

    public Thread getThread() {
        return thread;
    }

    public void cancel() {
        synchronized (this) {
            this.canceled = true;
            Thread locThread = thread;
            if (locThread != null && locThread != Thread.currentThread()) {
                getJob().getLogger().warning("Interrupt: " + solver + " : " + locThread);
                locThread.interrupt();
            } else if (locThread != Thread.currentThread()) {
                getJob().getLogger().warning("Could Not Interrupt: " + solver + " : " + locThread);
            }
        }
    }
}

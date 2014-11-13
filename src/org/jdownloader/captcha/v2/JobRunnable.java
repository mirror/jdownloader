package org.jdownloader.captcha.v2;

import java.util.concurrent.ScheduledExecutorService;

import jd.controlling.captcha.SkipException;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.solverjob.SolverJob;

public class JobRunnable<T> implements Runnable {

    private SolverJob<T>                         job;
    private ChallengeSolver<T>                   solver;
    private boolean                              canceled;

    public final static ScheduledExecutorService TIMINGQUEUE = DelayedRunnable.getNewScheduledExecutorService();

    public JobRunnable(ChallengeSolver<T> challengeSolver, SolverJob<T> job) {
        this.job = job;
        this.solver = challengeSolver;

    }

    public SolverJob<T> getJob() {
        return job;
    }

    public void fireTimeoutEvent() {
        if (!job.isDone(solver)) {
            solver.kill(job);
            job.fireTimeoutEvent(solver);

        }
    }

    @Override
    public void run() {

        try {
            synchronized (this) {
                if (canceled) {
                    return;
                }

                getJob().getLogger().info(solver + " is Active.");
                thread = Thread.currentThread();
                thread.setName(solver + "-Thread");
            }
            fireBeforeSolveEvent();

            DelayedRunnable timeout = null;
            // final Thread thread = Thread.currentThread();
            if (solver.getTimeout() > 0) {
                timeout = new DelayedRunnable(TIMINGQUEUE, solver.getTimeout()) {
                    @Override
                    public void delayedrun() {
                        System.out.println("Timeout!");
                        fireTimeoutEvent();
                    }

                };

            }

            try {

                if (timeout != null) {
                    timeout.resetAndStart();
                }
                // int waitTimeout = solver.getWaitForOthersTimeout();
                // ChallengeSolver<?>[] waitInstances = ChallengeResponseController.getInstance().getWaitForOtherSolversList(solver);
                // getJob().getLogger().info("Solver " + solver + " Waits " + TimeFormatter.formatMilliSeconds(waitTimeout, 0) + " for " +
                // Arrays.toString(waitInstances));
                // if (waitTimeout > 0 && waitInstances != null && waitInstances.length > 0) {
                // job.waitFor(waitTimeout, waitInstances);
                //
                // }
                // getJob().getLogger().info("Solver " + solver + " Waiting Done... run now.");
                long startedWaiting = System.currentTimeMillis();
                for (ChallengeSolver<?> s : job.getSolverList()) {
                    if (s != solver) {
                        int waitForThisSolver = solver.getService().getWaitForByID(s.getService().getID());
                        if (waitForThisSolver > 1000) {
                            job.getLogger().info(solver + " will wait up to " + TimeFormatter.formatMilliSeconds(waitForThisSolver, 0) + " for " + s);

                        }
                    }
                }
                for (ChallengeSolver<?> s : job.getSolverList()) {
                    if (s != solver) {
                        int waitForThisSolver = solver.getService().getWaitForByID(s.getService().getID());
                        waitForThisSolver -= (System.currentTimeMillis() - startedWaiting);
                        if (waitForThisSolver > 1000) {
                            long t = System.currentTimeMillis();
                            job.getLogger().info(solver + " now waits up to " + TimeFormatter.formatMilliSeconds(waitForThisSolver, 0) + " for " + s);
                            job.waitFor(waitForThisSolver, s);
                            job.getLogger().info(solver + " actually waited " + TimeFormatter.formatMilliSeconds(System.currentTimeMillis() - t, 0) + " for " + s);
                        }
                    }
                }

                solver.solve(job);

            } catch (SkipException e) {

                ChallengeResponseController.getInstance().setSkipRequest(e.getSkipRequest(), solver, job.getChallenge());

            } catch (Throwable e) {
                getJob().getLogger().log(e);

            } finally {
                if (timeout != null) {
                    timeout.stop();
                }
                fireDoneAndAfterSolveEvents();

            }
        } finally {
            thread = null;
        }
    }

    public void fireDoneAndAfterSolveEvents() {
        // order is important. listeners should have a chance to validate which solvers are done
        job.setSolverDone(solver);

        job.fireAfterSolveEvent(solver);
    }

    public void fireBeforeSolveEvent() {
        job.fireBeforeSolveEvent(solver);
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

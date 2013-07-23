package org.jdownloader.captcha.v2.solverjob;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import jd.controlling.captcha.CaptchaSettings;
import jd.controlling.captcha.SkipRequest;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.ChallengeSolver;

public class SolverJob<T> {

    private Challenge<T>                  challenge;

    private CaptchaSettings               config;

    private ChallengeResponseController   controller;
    private ArrayList<ResponseList<T>>    cumulatedList;
    private HashSet<ChallengeSolver<T>>   doneList  = new HashSet<ChallengeSolver<T>>();
    private ChallengeSolverJobEventSender eventSender;
    private List<AbstractResponse<T>>     responses = new ArrayList<AbstractResponse<T>>();
    // private HashSet<ChallengeSolver<T>> runningList = new HashSet<ChallengeSolver<T>>();
    private HashSet<ChallengeSolver<T>>   solverList;

    private LogSource                     logger;

    private SkipRequest                   skipRequest;

    private Object                        LOCK      = new Object();

    // private boolean canceled = false;

    public String toString() {

        return "CaptchaJob: " + challenge + " Solver: " + solverList;
    }

    public SolverJob(ChallengeResponseController controller, Challenge<T> c, List<ChallengeSolver<T>> solver) {
        this.challenge = c;
        this.controller = controller;
        this.solverList = new HashSet<ChallengeSolver<T>>(solver);
        config = JsonConfig.create(CaptchaSettings.class);

    }

    public void addAnswer(AbstractResponse<T> abstractResponse) {
        boolean kill = false;
        synchronized (LOCK) {
            responses.add(abstractResponse);
            challenge.setResult(cumulate());
            if (isSolved()) {
                getLogger().info("Is Solved - kill rest");
                kill = true;
            }
        }
        if (kill) kill();
        fireNewAnswerEvent(abstractResponse);
    }

    public ArrayList<ResponseList<T>> getResponses() {
        return cumulatedList;
    }

    public ResponseList<T> getResponse() {
        synchronized (LOCK) {
            ArrayList<ResponseList<T>> lst = cumulatedList;
            if (lst == null || lst.size() == 0) return null;
            return lst.get(0);
        }
    }

    public boolean areDone(ChallengeSolver<?>... instances) {
        synchronized (LOCK) {
            for (ChallengeSolver<?> cs : instances) {
                if (solverList.contains(cs) && !doneList.contains(cs)) { return false; }
            }
            logger.info("All: " + solverList);
            logger.info("Done: " + doneList);
        }
        return true;
    }

    private ResponseList<T> cumulate() {
        HashMap<Object, ResponseList<T>> map = new HashMap<Object, ResponseList<T>>();
        ArrayList<ResponseList<T>> list = new ArrayList<ResponseList<T>>();
        synchronized (LOCK) {
            for (AbstractResponse<T> a : responses) {
                ResponseList<T> cache = map.get(a.getValue());
                if (cache == null) {
                    cache = new ResponseList<T>();
                    list.add(cache);
                    map.put(a.getValue(), cache);
                }
                cache.add(a);
            }
        }
        Collections.sort(list);
        this.cumulatedList = list;
        return list.size() > 0 ? list.get(0) : null;
    }

    public void fireAfterSolveEvent(ChallengeSolver<T> solver) {

        controller.fireAfterSolveEvent(this, solver);

    }

    public void setSolverDone(ChallengeSolver<T> solver) {

        synchronized (LOCK) {
            if (!solverList.contains(solver)) throw new IllegalStateException("This Job does not contain this solver");
            if (!doneList.add(solver)) return;
            // runningList.remove(solver);
        }
        if (eventSender != null) eventSender.fireEvent(new ChallengeSolverJobEvent(this, ChallengeSolverJobEvent.Type.SOLVER_DONE, solver));
        synchronized (this) {
            this.notifyAll();
        }
    }

    public void fireBeforeSolveEvent(ChallengeSolver<T> solver) {
        synchronized (LOCK) {
            if (!solverList.contains(solver)) throw new IllegalStateException("This Job does not contain this solver");
        }
        if (eventSender != null) eventSender.fireEvent(new ChallengeSolverJobEvent(this, ChallengeSolverJobEvent.Type.SOLVER_START, solver));
        controller.fireBeforeSolveEvent(this, solver);
    }

    private void fireNewAnswerEvent(AbstractResponse<T> abstractResponse) {
        controller.fireNewAnswerEvent(this, abstractResponse);
        if (eventSender != null) eventSender.fireEvent(new ChallengeSolverJobEvent(this, ChallengeSolverJobEvent.Type.NEW_ANSWER, abstractResponse));

    }

    public void fireTimeoutEvent(ChallengeSolver<T> solver) {
        synchronized (LOCK) {
            if (!solverList.contains(solver)) throw new IllegalStateException("This Job does not contain this solver");
        }
        if (eventSender != null) eventSender.fireEvent(new ChallengeSolverJobEvent(this, ChallengeSolverJobEvent.Type.SOLVER_TIMEOUT, solver));
    }

    public Challenge<T> getChallenge() {
        return challenge;
    }

    public synchronized ChallengeSolverJobEventSender getEventSender() {
        if (eventSender == null) {
            eventSender = new ChallengeSolverJobEventSender();
        }
        return eventSender;
    }

    public boolean isDone() {
        synchronized (LOCK) {
            for (ChallengeSolver<T> sj : solverList) {
                if (!doneList.contains(sj)) {
                    if (sj.isJobDone(this)) {
                        setSolverDone(sj);
                    }
                }
            }
            return solverList.size() == doneList.size();
        }
    }

    public boolean isDone(ChallengeSolver<T> instance) {
        synchronized (LOCK) {
            if (instance.isJobDone(this)) {
                setSolverDone(instance);
            }
            return !solverList.contains(instance) || doneList.contains(instance);
        }
    }

    public boolean isSolved() {
        int autoPriority = config.getAutoCaptchaPriorityThreshold();
        ResponseList<T> response = getResponse();
        return (response != null && response.getSum() >= autoPriority);
    }

    public void kill() {
        ArrayList<ChallengeSolver<T>> killList = new ArrayList<ChallengeSolver<T>>();
        synchronized (LOCK) {
            for (ChallengeSolver<T> s : solverList) {
                if (!doneList.contains(s)) {
                    getLogger().info("Kill " + s);
                    killList.add(s);
                }
            }
        }
        for (ChallengeSolver<T> s : killList) {
            s.kill(this);
        }
    }

    public void waitFor(int timeout, ChallengeSolver<?>... instances) throws InterruptedException {
        long endTime = -1;
        if (timeout > 0) {
            endTime = System.currentTimeMillis() + timeout;
            logger.info(this + " Wait max" + timeout + " ms for " + instances);
        } else {
            logger.info(this + " Wait infinite for " + instances);
        }
        try {
            while (!areDone(instances)) {
                if (Thread.interrupted()) throw new InterruptedException(this + " got interrupted");
                if (isSolved()) throw new InterruptedException(this + " is Solved");
                synchronized (this) {
                    if (!areDone(instances)) {
                        if (endTime > 0) {
                            long timeToWait = endTime - System.currentTimeMillis();
                            if (timeToWait > 0) {
                                logger.info(this + " Wait " + timeToWait);
                                this.wait(timeToWait);
                            } else {
                                logger.info(this + " Timed Out! ");
                                return;
                            }
                        } else {
                            logger.info(this + " Wait infinite");
                            this.wait();
                        }
                        logger.info(this + " Wokeup");
                    }
                }
            }
            if (Thread.interrupted()) throw new InterruptedException(this + " got interrupted");
            if (isSolved()) throw new InterruptedException(this + " is Solved");
            logger.info("Exit " + this + " by done: " + areDone(instances));
        } catch (InterruptedException e) {
            logger.log(e);
            logger.info(this + " exit by interrupt");
            throw e;
        }

    }

    public void setLogger(LogSource logSource) {
        this.logger = logSource;
    }

    public LogSource getLogger() {
        return logger;
    }

    public SkipRequest getSkipRequest() {
        return skipRequest;
    }

    public void setSkipRequest(SkipRequest skipRequest) {
        boolean kill = false;
        synchronized (this) {

            if (this.skipRequest != null) return;
            this.skipRequest = skipRequest;
            if (skipRequest != null) {
                getLogger().info("Got Skip Request:" + skipRequest);
                kill = true;
            }
        }
        if (kill) kill();
    }

    // public void cancel() {
    // canceled = true;
    // synchronized (this) {
    // this.notifyAll();
    //
    // }
    // }

    // public boolean isCanceled() {
    // return canceled;
    // }

}

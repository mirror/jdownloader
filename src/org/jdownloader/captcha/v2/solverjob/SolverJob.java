package org.jdownloader.captcha.v2.solverjob;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.controlling.captcha.CaptchaSettings;
import jd.controlling.captcha.SkipRequest;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Exceptions;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.ValidationResult;

public class SolverJob<T> {
    private final Challenge<T>                            challenge;
    private final CaptchaSettings                         config;
    private final ChallengeResponseController             controller;
    private volatile ArrayList<ResponseList<T>>           cumulatedList;
    private final HashSet<ChallengeSolver<T>>             doneList  = new HashSet<ChallengeSolver<T>>();
    private volatile ChallengeSolverJobEventSender        eventSender;
    private final List<AbstractResponse<T>>               responses = new ArrayList<AbstractResponse<T>>();
    private final CopyOnWriteArraySet<ChallengeSolver<T>> solverList;
    private LogSource                                     logger;
    private volatile SkipRequest                          skipRequest;
    private final Object                                  LOCK      = new Object();
    private final AtomicBoolean                           alive     = new AtomicBoolean(true);
    private long                                          created;

    public String toString() {
        return "CaptchaJob: " + new Date(created) + " " + challenge + " Solver: " + solverList;
    }

    public SolverJob(ChallengeResponseController controller, Challenge<T> c, List<ChallengeSolver<T>> solver) {
        this.challenge = c;
        created = System.currentTimeMillis();
        this.controller = controller;
        this.solverList = new CopyOnWriteArraySet<ChallengeSolver<T>>(solver);
        config = JsonConfig.create(CaptchaSettings.class);
    }

    protected void log(String txt) {
        final LogSource lLogger = getLogger();
        if (lLogger != null) {
            lLogger.info(txt);
        }
    }

    public boolean addAnswer(AbstractResponse<T> abstractResponse) {
        if (!abstractResponse.getChallenge().validateResponse(abstractResponse)) {
            abstractResponse.setValidation(ValidationResult.INVALID);
            return false;
        }
        boolean kill = false;
        boolean isAlive;
        synchronized (LOCK) {
            isAlive = alive.get();
            if (isAlive) {
                responses.add(abstractResponse);
                challenge.setResult(cumulate());
                if (isSolved()) {
                    alive.set(false);
                    log("Is Solved - kill rest");
                    kill = true;
                }
            }
        }
        if (kill) {
            kill();
        }
        if (isAlive) {
            fireNewAnswerEvent(abstractResponse);
            return true;
        } else {
            abstractResponse.setValidation(ValidationResult.UNUSED);
            return false;
        }
    }

    public ArrayList<ResponseList<T>> getResponses() {
        return cumulatedList;
    }

    public ResponseList<T> getResponseAndKill() {
        synchronized (LOCK) {
            alive.set(false);
            final ArrayList<ResponseList<T>> lst = cumulatedList;
            if (lst == null || lst.size() == 0) {
                return null;
            }
            return lst.get(0);
        }
    }

    public ResponseList<T> getResponse() {
        synchronized (LOCK) {
            final ArrayList<ResponseList<T>> lst = cumulatedList;
            if (lst == null || lst.size() == 0) {
                return null;
            }
            return lst.get(0);
        }
    }

    public boolean areDone(ChallengeSolver<?>... instances) {
        synchronized (LOCK) {
            for (ChallengeSolver<?> cs : instances) {
                if (solverList.contains(cs) && !doneList.contains(cs)) {
                    return false;
                }
            }
            log("All: " + solverList);
            log("Done: " + doneList);
        }
        return true;
    }

    private ResponseList<T> cumulate() {
        final HashMap<Object, ResponseList<T>> map = new HashMap<Object, ResponseList<T>>();
        final ArrayList<ResponseList<T>> list = new ArrayList<ResponseList<T>>();
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
            if (!solverList.contains(solver)) {
                throw new IllegalStateException("This Job does not contain this solver");
            }
            if (!doneList.add(solver)) {
                return;
            }
        }
        if (eventSender != null) {
            eventSender.fireEvent(new ChallengeSolverJobEvent(this, ChallengeSolverJobEvent.Type.SOLVER_DONE, solver));
        }
        synchronized (this) {
            this.notifyAll();
        }
    }

    public void fireBeforeSolveEvent(ChallengeSolver<T> solver) {
        synchronized (LOCK) {
            if (!solverList.contains(solver)) {
                throw new IllegalStateException("This Job does not contain this solver");
            }
        }
        if (eventSender != null) {
            eventSender.fireEvent(new ChallengeSolverJobEvent(this, ChallengeSolverJobEvent.Type.SOLVER_START, solver));
        }
        controller.fireBeforeSolveEvent(this, solver);
    }

    private void fireNewAnswerEvent(AbstractResponse<T> abstractResponse) {
        controller.fireNewAnswerEvent(this, abstractResponse);
        if (eventSender != null) {
            eventSender.fireEvent(new ChallengeSolverJobEvent(this, ChallengeSolverJobEvent.Type.NEW_ANSWER, abstractResponse));
        }
    }

    public void fireTimeoutEvent(ChallengeSolver<T> solver) {
        synchronized (LOCK) {
            if (!solverList.contains(solver)) {
                throw new IllegalStateException("This Job does not contain this solver");
            }
        }
        if (eventSender != null) {
            eventSender.fireEvent(new ChallengeSolverJobEvent(this, ChallengeSolverJobEvent.Type.SOLVER_TIMEOUT, solver));
        }
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
        final int autoPriority = config.getAutoCaptchaPriorityThreshold();
        final ResponseList<T> response = getResponse();
        return (response != null && response.getSum() >= autoPriority);
    }

    public boolean isAlive() {
        return alive.get();
    }

    public void kill() {
        final ArrayList<ChallengeSolver<T>> killList = new ArrayList<ChallengeSolver<T>>();
        synchronized (LOCK) {
            alive.set(false);
            for (ChallengeSolver<T> s : solverList) {
                if (!doneList.contains(s)) {
                    log("Kill " + s);
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
            log(this + " Wait max" + timeout + " ms for " + instances);
        } else {
            log(this + " Wait infinite for " + instances);
        }
        try {
            while (!areDone(instances)) {
                if (isSolved()) {
                    throw new InterruptedException(this + " is Solved");
                } else if (Thread.interrupted()) {
                    throw new InterruptedException(this + " got interrupted");
                }
                synchronized (this) {
                    if (!areDone(instances)) {
                        if (endTime > 0) {
                            long timeToWait = endTime - System.currentTimeMillis();
                            if (timeToWait > 0) {
                                log(this + " Wait " + timeToWait);
                                this.wait(timeToWait);
                            } else {
                                log(this + " Timed Out! ");
                                return;
                            }
                        } else {
                            log(this + " Wait infinite");
                            this.wait();
                        }
                        log(this + " Wokeup");
                    }
                }
            }
            if (isSolved()) {
                throw new InterruptedException(this + " is Solved");
            } else if (Thread.interrupted()) {
                throw new InterruptedException(this + " got interrupted");
            }
            log("Exit " + this + " by done: " + areDone(instances));
        } catch (InterruptedException e) {
            log(Exceptions.getStackTrace(e));
            log(this + " exit by interrupt");
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

    public boolean setSkipRequest(SkipRequest skipRequest) {
        boolean kill = false;
        synchronized (LOCK) {
            if (alive.compareAndSet(true, false)) {
                this.skipRequest = skipRequest;
                if (skipRequest != null) {
                    log("Got Skip Request:" + skipRequest);
                }
                kill = true;
            }
        }
        if (kill) {
            kill();
        }
        return kill;
    }

    public Collection<ChallengeSolver<T>> getSolverList() {
        synchronized (LOCK) {
            return Collections.unmodifiableCollection(solverList);
        }
    }

    /**
     * call to tell the job, that the result has been correct
     */
    public void validate() {
        final ArrayList<ResponseList<T>> responsesLists = this.getResponses();
        if (responsesLists != null) {
            for (int i = 0; i < responsesLists.size(); i++) {
                final ResponseList<T> responseList = responsesLists.get(0);
                for (final AbstractResponse<T> response : responseList) {
                    if (i == 0) {
                        // used response
                        response.setValidation(ValidationResult.VALID);
                    } else {
                        // unused responses
                        // maybe send invalid instead?
                        response.setValidation(ValidationResult.UNUSED);
                    }
                }
            }
        }
    }

    /**
     * call to tell the job, that the result has been INCORRECT
     */
    public void invalidate() {
        final ArrayList<ResponseList<T>> responsesLists = this.getResponses();
        if (responsesLists != null) {
            for (int i = 0; i < responsesLists.size(); i++) {
                final ResponseList<T> responseList = responsesLists.get(0);
                for (final AbstractResponse<T> response : responseList) {
                    if (i == 0) {
                        // used response
                        response.setValidation(ValidationResult.INVALID);
                    } else {
                        // unused responses
                        response.setValidation(ValidationResult.UNUSED);
                    }
                }
            }
        }
    }
}

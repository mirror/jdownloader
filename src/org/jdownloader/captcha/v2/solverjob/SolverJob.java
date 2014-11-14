package org.jdownloader.captcha.v2.solverjob;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.controlling.captcha.CaptchaSettings;
import jd.controlling.captcha.SkipRequest;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Exceptions;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.ChallengeResponseValidation;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.statistics.StatsManager;

public class SolverJob<T> {

    private final Challenge<T>                     challenge;

    private final CaptchaSettings                  config;

    private final ChallengeResponseController      controller;
    private volatile ArrayList<ResponseList<T>>    cumulatedList;
    private final HashSet<ChallengeSolver<T>>      doneList  = new HashSet<ChallengeSolver<T>>();
    private volatile ChallengeSolverJobEventSender eventSender;
    private final List<AbstractResponse<T>>        responses = new ArrayList<AbstractResponse<T>>();
    // private HashSet<ChallengeSolver<T>> runningList = new HashSet<ChallengeSolver<T>>();
    private final HashSet<ChallengeSolver<T>>      solverList;

    private LogSource                              logger;

    private volatile SkipRequest                   skipRequest;

    private final Object                           LOCK      = new Object();

    private final AtomicBoolean                    alive     = new AtomicBoolean(true);

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

    protected void log(String txt) {
        final LogSource lLogger = getLogger();
        if (lLogger != null) {
            lLogger.info(txt);
        }
    }

    public void addAnswer(AbstractResponse<T> abstractResponse) {
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
        } else {
            final Object solver = abstractResponse.getSolver();
            if (solver instanceof ChallengeResponseValidation) {
                try {
                    ((ChallengeResponseValidation) solver).setUnused(abstractResponse, this);
                } catch (final Throwable e) {
                    LogSource.exception(getLogger(), e);
                }
            }
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
        int autoPriority = config.getAutoCaptchaPriorityThreshold();
        ResponseList<T> response = getResponse();
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
                if (Thread.interrupted()) {
                    throw new InterruptedException(this + " got interrupted");
                }
                if (isSolved()) {
                    throw new InterruptedException(this + " is Solved");
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
            if (Thread.interrupted()) {
                throw new InterruptedException(this + " got interrupted");
            }
            if (isSolved()) {
                throw new InterruptedException(this + " is Solved");
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

    public void setSkipRequest(SkipRequest skipRequest) {
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
    }

    public Collection<ChallengeSolver<T>> getSolverList() {
        synchronized (LOCK) {
            return Collections.unmodifiableCollection(solverList);
        }
    }

    private boolean sameResponseValue(AbstractResponse a, AbstractResponse b) {
        if (a != null && b != null && a.getValue() != null && b.getValue() != null) {
            return a.getValue() == b.getValue() || a.getValue().equals(b.getValue());
        }
        return false;
    }

    /**
     * call to tell the job, that the result has been correct
     */
    public void validate() {
        StatsManager.I().logCaptcha(this);
        final ResponseList<T> returnedResponseList = this.getResponse();
        if (returnedResponseList != null && returnedResponseList.size() > 0) {
            final AbstractResponse<?> returnedResponse = returnedResponseList.get(0);
            for (final AbstractResponse<T> response : returnedResponseList) {
                if (response.getSolver() instanceof ChallengeResponseValidation) {
                    final ChallengeResponseValidation validation = (ChallengeResponseValidation) response.getSolver();
                    if (returnedResponse == response) {
                        try {
                            validation.setValid(response, this);
                        } catch (final Throwable e) {
                            LogSource.exception(getLogger(), e);
                        }
                    } else {
                        if (sameResponseValue(returnedResponse, response)) {
                            try {
                                validation.setValid(response, this);
                            } catch (final Throwable e) {
                                LogSource.exception(getLogger(), e);
                            }
                        } else {
                            try {
                                validation.setInvalid(response, this);
                            } catch (final Throwable e) {
                                LogSource.exception(getLogger(), e);
                            }
                        }
                    }
                }
            }
            final ArrayList<ResponseList<T>> allResponseLists = this.getResponses();
            if (allResponseLists != null) {
                for (ResponseList<T> responseList : allResponseLists) {
                    if (responseList != returnedResponseList) {
                        for (final AbstractResponse<T> response : responseList) {
                            if (response.getSolver() instanceof ChallengeResponseValidation) {
                                final ChallengeResponseValidation validation = (ChallengeResponseValidation) response.getSolver();
                                if (sameResponseValue(returnedResponse, response)) {
                                    try {
                                        validation.setValid(response, this);
                                    } catch (final Throwable e) {
                                        LogSource.exception(getLogger(), e);
                                    }
                                } else {
                                    try {
                                        validation.setInvalid(response, this);
                                    } catch (final Throwable e) {
                                        LogSource.exception(getLogger(), e);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * call to tell the job, that the result has been INCORRECT
     */
    public void invalidate() {
        final ResponseList<T> returnedResponseList = this.getResponse();
        if (returnedResponseList != null && returnedResponseList.size() > 0) {
            final AbstractResponse<?> returnedResponse = returnedResponseList.get(0);
            for (final AbstractResponse<T> response : returnedResponseList) {
                if (response.getSolver() instanceof ChallengeResponseValidation) {
                    final ChallengeResponseValidation validation = (ChallengeResponseValidation) response.getSolver();
                    if (returnedResponse == response) {
                        try {
                            validation.setInvalid(response, this);
                        } catch (final Throwable e) {
                            LogSource.exception(getLogger(), e);
                        }
                    } else {
                        if (sameResponseValue(returnedResponse, response)) {
                            try {
                                validation.setInvalid(response, this);
                            } catch (final Throwable e) {
                                LogSource.exception(getLogger(), e);
                            }
                        } else {
                            try {
                                validation.setUnused(response, this);
                            } catch (final Throwable e) {
                                LogSource.exception(getLogger(), e);
                            }
                        }
                    }
                }
            }
            final ArrayList<ResponseList<T>> allResponseLists = this.getResponses();
            if (allResponseLists != null) {
                for (ResponseList<T> responseList : allResponseLists) {
                    if (responseList != returnedResponseList) {
                        for (final AbstractResponse<T> response : responseList) {
                            if (response.getSolver() instanceof ChallengeResponseValidation) {
                                final ChallengeResponseValidation validation = (ChallengeResponseValidation) response.getSolver();
                                if (sameResponseValue(returnedResponse, response)) {
                                    try {
                                        validation.setInvalid(response, this);
                                    } catch (final Throwable e) {
                                        LogSource.exception(getLogger(), e);
                                    }
                                } else {
                                    try {
                                        validation.setUnused(response, this);
                                    } catch (final Throwable e) {
                                        LogSource.exception(getLogger(), e);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

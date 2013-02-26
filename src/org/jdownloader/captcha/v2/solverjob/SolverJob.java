package org.jdownloader.captcha.v2.solverjob;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import jd.controlling.captcha.CaptchaSettings;

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
    private HashSet<ChallengeSolver<T>>   doneList    = new HashSet<ChallengeSolver<T>>();
    private ChallengeSolverJobEventSender eventSender;
    private List<AbstractResponse<T>>     responses   = new ArrayList<AbstractResponse<T>>();
    private HashSet<ChallengeSolver<T>>   runningList = new HashSet<ChallengeSolver<T>>();
    private HashSet<ChallengeSolver<T>>   solverList;

    private LogSource                     logger;

    private boolean                       canceled    = false;

    public String toString() {

        return "CaptchaJob: " + challenge;
    }

    public SolverJob(ChallengeResponseController controller, Challenge<T> c, List<ChallengeSolver<T>> solver) {
        this.challenge = c;
        this.controller = controller;
        this.solverList = new HashSet<ChallengeSolver<T>>(solver);
        config = JsonConfig.create(CaptchaSettings.class);

    }

    public void addAnswer(AbstractResponse<T> abstractResponse) {
        responses.add(abstractResponse);
        this.cumulate();
        challenge.setResult(cumulatedList.get(0).getValue());
        if (isSolved()) {
            getLogger().info("Is Solved - kill rest");
            kill();
        }
        fireNewAnswerEvent(abstractResponse);
    }

    public ArrayList<ResponseList<T>> getResponses() {
        return cumulatedList;
    }

    public ResponseList<T> getResponse() {
        if (cumulatedList == null || cumulatedList.size() == 0) return null;
        return cumulatedList.get(0);
    }

    public boolean areDone(ChallengeSolver<?>[] instances) {

        for (ChallengeSolver<?> cs : instances) {
            if (solverList.contains(cs) && !doneList.contains(cs)) { return false; }
        }
        logger.info(solverList + "");
        logger.info(doneList + "");
        return true;
    }

    private void cumulate() {
        HashMap<Object, ResponseList<T>> map = new HashMap<Object, ResponseList<T>>();
        ArrayList<ResponseList<T>> list = new ArrayList<ResponseList<T>>();
        for (AbstractResponse<T> a : responses) {

            ResponseList<T> cache = map.get(a.getValue());
            if (cache == null) {
                cache = new ResponseList<T>();
                list.add(cache);
                map.put(a.getValue(), cache);

            }

            cache.add(a);
        }
        Collections.sort(list);
        this.cumulatedList = list;
    }

    public void fireAfterSolveEvent(ChallengeSolver<T> solver) {
        if (!solverList.contains(solver)) throw new IllegalStateException("This Job does not contain this solver");
        doneList.add(solver);
        runningList.remove(solver);
        if (eventSender != null) eventSender.fireEvent(new ChallengeSolverJobEvent(this, ChallengeSolverJobEvent.Type.SOLVER_DONE, solver));
        controller.fireAfterSolveEvent(this, solver);
        logger.info("Notify");
        synchronized (this) {
            this.notifyAll();

        }

    }

    public void fireBeforeSolveEvent(ChallengeSolver<T> solver) {
        if (!solverList.contains(solver)) throw new IllegalStateException("This Job does not contain this solver");
        runningList.add(solver);
        if (eventSender != null) eventSender.fireEvent(new ChallengeSolverJobEvent(this, ChallengeSolverJobEvent.Type.SOLVER_START, solver));
        controller.fireBeforeSolveEvent(this, solver);
    }

    private void fireNewAnswerEvent(AbstractResponse<T> abstractResponse) {
        controller.fireNewAnswerEvent(this, abstractResponse);
        if (eventSender != null) eventSender.fireEvent(new ChallengeSolverJobEvent(this, ChallengeSolverJobEvent.Type.NEW_ANSWER, abstractResponse));

    }

    public void fireTimeoutEvent(ChallengeSolver<T> solver) {
        if (!solverList.contains(solver)) throw new IllegalStateException("This Job does not contain this solver");
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
        return isCanceled() || solverList.size() == doneList.size();
    }

    public boolean isDone(ChallengeSolver<T> instance) {
        return !solverList.contains(instance) || doneList.contains(instance);
    }

    public boolean isSolved() {
        return isSolved(config.getAutoCaptchaErrorTreshold());
    }

    private boolean isSolved(int treshhold) {

        if (cumulatedList == null || cumulatedList.size() == 0) return false;
        return cumulatedList.get(0).getSum() >= treshhold;
    }

    private void kill() {
        for (ChallengeSolver<T> s : solverList) {
            if (!doneList.contains(s)) {
                getLogger().info("Kill " + s);
                s.kill(this);
            }
        }
    }

    public void waitFor(int timeout, ChallengeSolver<?>... instances) throws InterruptedException {

        long start = System.currentTimeMillis();
        while (!areDone(instances)) {
            synchronized (this) {
                if (!areDone(instances)) {
                    logger.info("Wait " + timeout);
                    if (timeout > 0) {
                        this.wait(timeout);
                    } else {
                        this.wait();
                    }

                    logger.info("Wokeup");
                    if (System.currentTimeMillis() - start > timeout && timeout > 0) {
                        logger.info("Timed Out! ");
                        return;
                    }
                }
            }
        }

    }

    public void setLogger(LogSource logSource) {
        this.logger = logSource;
    }

    public LogSource getLogger() {
        return logger;
    }

    public void cancel() {
        canceled = true;
        synchronized (this) {
            this.notifyAll();

        }
    }

    public boolean isCanceled() {
        return canceled;
    }

}

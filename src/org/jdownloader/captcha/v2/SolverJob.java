package org.jdownloader.captcha.v2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import jd.controlling.captcha.CaptchaSettings;

import org.appwork.storage.config.JsonConfig;

public class SolverJob<T> {

    private Challenge<T> challenge;

    public Challenge<T> getChallenge() {
        return challenge;
    }

    private HashSet<ChallengeSolver>   solverList;
    private HashSet<ChallengeSolver>   done      = new HashSet<ChallengeSolver>();
    private List<AbstractResponse<T>>  responses = new ArrayList<AbstractResponse<T>>();
    private CaptchaSettings            config;
    private ArrayList<ResponseList<T>> cumulatedList;

    public SolverJob(Challenge<T> c, List<ChallengeSolver> solver) {
        this.challenge = c;
        this.solverList = new HashSet<ChallengeSolver>(solver);
        config = JsonConfig.create(CaptchaSettings.class);

    }

    public void setDone(ChallengeSolver cs) {
        if (!solverList.contains(cs)) throw new IllegalStateException("This Job does not contain this solver");
        done.add(cs);
    }

    public void addAnswer(AbstractResponse<T> AbstractResponse) {
        responses.add(AbstractResponse);
        this.cumulate();
        challenge.setResult(cumulatedList.get(0).getValue());
    }

    public void waitFor(ChallengeSolver<?>... solver) throws InterruptedException {

        // todo: optimize...this is a job for synchro king. Jiaz
        while (true) {
            boolean done = true;
            for (ChallengeSolver<?> s : solver) {
                if (solverList.contains(s) && !this.done.contains(s)) {
                    done = false;
                    break;
                }
            }
            if (done) return;
            Thread.sleep(1000);
        }
    }

    public boolean isSolved() {
        return isSolved(config.getAutoCaptchaErrorTreshold());
    }

    private boolean isSolved(int treshhold) {

        if (cumulatedList == null || cumulatedList.size() == 0) return false;
        return false;
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
        this.cumulatedList = list;
    }

    public boolean isDone() {
        return solverList.size() == done.size();
    }
}

package org.jdownloader.captcha.v2.solver.solver9kw;

import org.appwork.utils.StringUtils;

public class NineKWAccount {
    private int solved;
    private int requests;
    private int skipped;

    public int getSolved() {
        return solved;
    }

    public void setSolved(int solved) {
        this.solved = solved;
    }

    public int getAnswered() {
        return answered;
    }

    public void setAnswered(int answered) {
        this.answered = answered;
    }

    private int    answered;
    private int    creditBalance;
    private String error;
    private int    worker;
    private int    workerMouse;
    private int    workerConfirm;
    private int    workerText;

    public int getWorkerText() {
        return workerText;
    }

    public void setWorkerText(int workerText) {
        this.workerText = workerText;
    }

    public int getWorkerMouse() {
        return workerMouse;
    }

    public void setWorkerMouse(int workerMouse) {
        this.workerMouse = workerMouse;
    }

    public int getWorkerConfirm() {
        return workerConfirm;
    }

    public void setWorkerConfirm(int workerConfirm) {
        this.workerConfirm = workerConfirm;
    }

    public int getAvgSolvtime() {
        return avgSolvtime;
    }

    public void setAvgSolvtime(int avgSolvtime) {
        this.avgSolvtime = avgSolvtime;
    }

    public int getQueue() {
        return queue;
    }

    public void setQueue(int queue) {
        this.queue = queue;
    }

    public int getQueue1() {
        return queue1;
    }

    public void setQueue1(int queue1) {
        this.queue1 = queue1;
    }

    public int getQueue2() {
        return queue2;
    }

    public void setQueue2(int queue2) {
        this.queue2 = queue2;
    }

    public int getWorker() {
        return worker;
    }

    private int inWork;

    public int getInWork() {
        return inWork;
    }

    public void setInWork(int inWork) {
        this.inWork = inWork;
    }

    private int avgSolvtime;
    private int queue;
    private int queue1;
    private int queue2;

    public String getError() {
        return error;
    }

    public void setCreditBalance(int creditBalance) {
        this.creditBalance = creditBalance;
    }

    public boolean isValid() {
        return StringUtils.isEmpty(error);
    }

    public int getCreditBalance() {
        return creditBalance;
    }

    public void setError(String credits) {
        error = credits;
    }

    public void setWorker(int worker) {
        this.worker = worker;
    }

    public int getRequests() {
        return requests;
    }

    public void setRequests(int requests) {
        this.requests = requests;
    }

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }
}

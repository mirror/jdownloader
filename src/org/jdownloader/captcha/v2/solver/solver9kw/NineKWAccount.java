package org.jdownloader.captcha.v2.solver.solver9kw;

import org.appwork.utils.StringUtils;

public class NineKWAccount {
    private int solved;
    private int solved9kw;
    private int requests;
    private int skipped;
    private int answered;
    private int answered9kw;
    private int Send;
    private int SendError;
    private int OK;
    private int NotOK;
    private int Unused;

    public int getSend() {
        return Send;
    }

    public void setSend(int Send) {
        this.Send = Send;
    }

    public int getSendError() {
        return SendError;
    }

    public void setSendError(int SendError) {
        this.SendError = SendError;
    }

    public int getOK() {
        return OK;
    }

    public void setOK(int OK) {
        this.OK = OK;
    }

    public int getNotOK() {
        return NotOK;
    }

    public void setNotOK(int NotOK) {
        this.NotOK = NotOK;
    }

    public int getUnused() {
        return Unused;
    }

    public void setUnused(int Unused) {
        this.Unused = Unused;
    }

    public int getSolved() {
        return solved;
    }

    public void setSolved(int solved) {
        this.solved = solved;
    }

    public int getSolved9kw() {
        return solved9kw;
    }

    public void setSolved9kw(int solved) {
        this.solved9kw = solved;
    }

    public int getAnswered() {
        return answered;
    }

    public void setAnswered(int answered) {
        this.answered = answered;
    }

    public int getAnswered9kw() {
        return answered9kw;
    }

    public void setAnswered9kw(int answered) {
        this.answered9kw = answered;
    }

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

package org.jdownloader.captcha.v2;

import org.jdownloader.controlling.UniqueAlltimeID;

public class AbstractResponse<T> {
    private final UniqueAlltimeID id = new UniqueAlltimeID();
    private int                   priority;

    public int getPriority() {
        return priority;
    }

    public UniqueAlltimeID getId() {
        return id;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    private T                  value;
    private final Object       solver;
    private final Challenge<T> challenge;

    public Object getSolver() {
        return solver;
    }

    public AbstractResponse(Challenge<T> challenge, Object solver, int priority, T responseData) {
        this.solver = solver;
        this.priority = priority;
        this.value = responseData;
        this.challenge = challenge;
    }

    public String toString() {
        return getClass().getSimpleName() + ": Value:" + value + " Priority: " + priority + " Solved By: " + solver;
    }

    public Challenge<T> getChallenge() {
        return challenge;
    }

}

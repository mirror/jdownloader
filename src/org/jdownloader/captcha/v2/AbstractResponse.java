package org.jdownloader.captcha.v2;

public class AbstractResponse<T> {

    private int priority;

    public int getPriority() {
        return priority;
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

    private T      value;
    private Object solver;

    public Object getSolver() {
        return solver;
    }

    public AbstractResponse(Object solver, int priority, T responseData) {
        this.solver = solver;
        this.priority = priority;
        this.value = responseData;
    }

    public String toString() {
        return getClass().getSimpleName() + ": Value:" + value + " Priority: " + priority + " Solved By: " + solver;
    }

}

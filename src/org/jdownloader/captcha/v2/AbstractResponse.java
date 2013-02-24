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

    private T value;

    public AbstractResponse(int priority, T responseData) {
        this.priority = priority;
        this.value = responseData;
    }

}

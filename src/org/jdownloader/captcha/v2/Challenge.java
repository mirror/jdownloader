package org.jdownloader.captcha.v2;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.jdownloader.controlling.UniqueAlltimeID;

public abstract class Challenge<T> {
    private final UniqueAlltimeID id = new UniqueAlltimeID();

    public UniqueAlltimeID getId() {
        return id;
    }

    public Challenge(String method, String explain2) {
        typeID = method;
        explain = explain2;

    }

    public Class<?> getResultType() {
        final Type superClass = this.getClass().getGenericSuperclass();
        if (superClass instanceof Class) { throw new IllegalArgumentException("Wrong Construct"); }
        return (Class<?>) ((ParameterizedType) superClass).getActualTypeArguments()[0];
    }

    abstract public boolean isSolved();

    private String typeID;
    private String explain;

    public String getTypeID() {
        return typeID;
    }

    public void setTypeID(String typeID) {
        this.typeID = typeID;
    }

    public String getExplain() {
        return explain;
    }

    public void setExplain(String explain) {
        this.explain = explain;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    private T result;
}

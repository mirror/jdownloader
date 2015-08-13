package jd.plugins.components;

public interface ThrowingRunnable<S extends Throwable> {

    public void run() throws S;
}

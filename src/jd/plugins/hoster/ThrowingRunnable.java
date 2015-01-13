package jd.plugins.hoster;

public interface ThrowingRunnable<S extends Throwable> {

    public void run() throws S;
}

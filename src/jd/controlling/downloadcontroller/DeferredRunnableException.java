package jd.controlling.downloadcontroller;

public class DeferredRunnableException extends Exception {

    protected final ExceptionRunnable exceptionRunnable;

    public ExceptionRunnable getExceptionRunnable() {
        return exceptionRunnable;
        //
    }

    public DeferredRunnableException(ExceptionRunnable exceptionRunnable) {
        this.exceptionRunnable = exceptionRunnable;
    }
}

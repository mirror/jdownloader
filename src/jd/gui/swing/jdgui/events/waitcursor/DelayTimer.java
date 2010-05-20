package jd.gui.swing.jdgui.events.waitcursor;

/**
 * This class implements a delay timer that will call trigger() on the
 * DelayTimerCallback delay milliseconds after startTimer() was called, if
 * stopTimer() was not called first. The timer will only throw events after
 * startTimer() is called. Until then, it does nothing. It is safe to call
 * stopTimer() and startTimer() repeatedly.
 * 
 * Note that calls to trigger() will happen on the timer thread.
 * 
 * This class is multiple-thread safe.
 * 
 * @author Based on the <a
 *         href="http://www.javaspecialists.eu/archive/Issue075.html">The Java
 *         Specialists' Newsletter</a>
 */
public class DelayTimer extends Thread {

    private final DelayTimerCallback callback;
    private final Object mutex = new Object();
    private final Object triggeredMutex = new Object();
    private final long delay;
    private boolean quit;
    private boolean triggered;
    private long waitTime;

    public DelayTimer(DelayTimerCallback callback, long delay) {
        this.callback = callback;
        this.delay = delay;
        setDaemon(true);
        start();
    }

    /**
     * Calling this method twice will reset the timer.
     */
    public void startTimer() {
        synchronized (mutex) {
            waitTime = delay;
            mutex.notify();
        }
    }

    public void stopTimer() {
        try {
            synchronized (mutex) {
                synchronized (triggeredMutex) {
                    if (triggered) {
                        triggeredMutex.wait();
                    }
                }
                waitTime = 0;
                mutex.notify();
            }
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            while (!quit) {
                synchronized (mutex) {
                    /*
                     * we rely on wait(0) being implemented to wait forever here
                     */
                    if (waitTime < 0) {
                        triggered = true;
                        waitTime = 0;
                    } else {
                        long saveWaitTime = waitTime;
                        waitTime = -1;
                        mutex.wait(saveWaitTime);
                    }
                }
                try {
                    if (triggered) {
                        callback.trigger();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    synchronized (triggeredMutex) {
                        triggered = false;
                        triggeredMutex.notify();
                    }
                }
            }
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
    }

    public void quit() {
        synchronized (mutex) {
            this.quit = true;
            mutex.notify();
        }
    }

}

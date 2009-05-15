package jd.gui.skins.simple;

import java.awt.EventQueue;

import javax.swing.SwingUtilities;

/**
 * This calss invokes a Runnable EDT Save and is able to return values.
 * 
 * @author coalado
 * 
 */
public abstract class GuiRunnable<T> implements Runnable {
    /**
     * 
     */
    private static final long serialVersionUID = 7777074589566807490L;

    private T returnValue;
    private final Object lock = new Object();

    private boolean started = false;
    private boolean done = false;

    public boolean isStarted() {
        return started;
    }

    private void setStarted(boolean started) {
        this.started = started;
    }

    /**
     * If this method mis calls, the thread waits until THD EDT has innvoked the
     * runnable.. it ensures that the return value is available
     * 
     * @return
     */
    public T getReturnValue() {

        waitForEDT();
        return returnValue;
    }

    /**
     * This method waits until the EDT has invoked the runnable. If the Runnable
     * is not started yet.. the start method gets called
     */
    public void waitForEDT() {
        if (done) return;
        if (!isStarted()) start();
        if (!SwingUtilities.isEventDispatchThread()) {
            synchronized (lock) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    //jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
                }
            }
        }
        done = true;
    }

    public void run() {
        this.returnValue = this.runSave();
        synchronized (lock) {
            lock.notify();
        }
    }

    abstract public T runSave();

    /**
     * Starts the Runnable and adds it to the ETD
     */
    public void start() {
        setStarted(true);
        if (SwingUtilities.isEventDispatchThread()) {
            run();
        } else {
            EventQueue.invokeLater(this);

        }

    }
}

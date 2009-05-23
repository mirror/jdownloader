//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

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
    private long id;

    /**
     * 
     */

    public GuiRunnable() {
        this.id = System.currentTimeMillis();
        System.out.println("Created " + id);
    }

    private static final long serialVersionUID = 7777074589566807490L;

    private T returnValue;
    private Object lock = new Object();

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
            if (lock != null) {
                synchronized (lock) {
                    try {
                        System.out.println(id + " lock ");
                        if (lock != null) {
                            lock.wait();
                        }else{
                            System.out.println(id + " concurrent Thread faster ");
                        }
                        System.out.println(id + " unlocked ");
                    } catch (InterruptedException e) {
                        // jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,
                        // "Exception occured", e);
                    }
                }
            }else{
                System.out.println(id + " concurrent Thread faster ");
            }
            
        }
        System.out.println(id + " return ");
        done = true;
    }

    public void run() {
        System.out.println(id + " run ");
        this.returnValue = this.runSave();
        System.out.println(id + " finished ");
        synchronized (lock) {
            System.out.println(id + " notify ");
            lock.notify();
            lock = null;
        }
    }

    abstract public T runSave();

    /**
     * Starts the Runnable and adds it to the ETD
     */
    public void start() {
        // new Exception().printStackTrace();
        System.out.println(id + " Started ");
        setStarted(true);
        if (SwingUtilities.isEventDispatchThread()) {
            System.out.println(id + " run direct ");
            run();
        } else {
            System.out.println(id + " queue ");
            EventQueue.invokeLater(this);

        }

    }
}

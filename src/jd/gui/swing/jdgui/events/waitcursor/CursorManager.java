package jd.gui.swing.jdgui.events.waitcursor;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.Stack;

/**
 * @author Based on <a
 *         href="http://www.javaspecialists.eu/archive/Issue075.html">The Java
 *         Specialists' Newsletter</a>
 */
public class CursorManager {

    private final DelayTimer waitTimer;
    private final Stack<DispatchedEvent> dispatchedEvents;
    private boolean needsCleanup;

    public CursorManager(DelayTimer waitTimer) {
        this.dispatchedEvents = new Stack<DispatchedEvent>();
        this.waitTimer = waitTimer;
    }

    private void cleanUp() {
        if (dispatchedEvents.peek().resetCursor()) {
            clearQueueOfInputEvents();
        }
    }

    private void clearQueueOfInputEvents() {
        EventQueue q = Toolkit.getDefaultToolkit().getSystemEventQueue();
        synchronized (q) {
            ArrayList<AWTEvent> nonInputEvents = gatherNonInputEvents(q);
            for (AWTEvent event : nonInputEvents) {
                q.postEvent(event);
            }
        }
    }

    private ArrayList<AWTEvent> gatherNonInputEvents(EventQueue systemQueue) {
        ArrayList<AWTEvent> events = new ArrayList<AWTEvent>();
        while (systemQueue.peekEvent() != null) {
            try {
                AWTEvent nextEvent = systemQueue.getNextEvent();
                if (!(nextEvent instanceof InputEvent)) {
                    events.add(nextEvent);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        return events;
    }

    public void push(Object source) {
        if (needsCleanup) {
            waitTimer.stopTimer();
            /*
             * this corrects the state when a modal dialog opened last time
             * round
             */
            cleanUp();
        }
        dispatchedEvents.push(new DispatchedEvent(source));
        needsCleanup = true;
    }

    public void pop() {
        cleanUp();
        dispatchedEvents.pop();
        if (!dispatchedEvents.isEmpty()) {
            /*
             * this will be stopped if getNextEvent() is called - used to watch
             * for modal dialogs closing
             */
            waitTimer.startTimer();
        } else {
            needsCleanup = false;
        }
    }

    public void setCursor() {
        dispatchedEvents.peek().setCursor();
    }

}

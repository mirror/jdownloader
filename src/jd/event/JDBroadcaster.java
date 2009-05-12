package jd.event;

import java.util.EventListener;
import java.util.Vector;

public abstract class JDBroadcaster<T extends EventListener, TT extends JDEvent> {

    transient protected Vector<T> callList = null;

    transient protected Vector<T> removeList = null;

    public JDBroadcaster() {
        callList = new Vector<T>();
        removeList = new Vector<T>();
    }

    public void addListener(T listener) {
        synchronized (callList) {
            synchronized (removeList) {
                if (removeList.contains(listener)) {
                    removeList.remove(listener);
                }
            }
            if (!callList.contains(listener)) callList.add(listener);
        }
    }

    public boolean hasListener() {
        return callList.size() > 0;
    }

    public boolean fireEvent(TT event) {
        // System.out.println("Broadcast start" + this.getClass());
        synchronized (callList) {
            synchronized (removeList) {
                callList.removeAll(removeList);
                removeList.clear();
            }
            for (int i = callList.size() - 1; i >= 0; i--) {
                this.fireEvent(callList.get(i), event);
            }
        }
        return false;
        // System.out.println("Broadcast stop" + this.getClass());
    }

    protected abstract void fireEvent(T listener, TT event);

    public void removeListener(T listener) {
        synchronized (removeList) {
            if (!removeList.contains(listener)) removeList.add(listener);
        }
    }
}

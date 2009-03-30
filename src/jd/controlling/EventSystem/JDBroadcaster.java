package jd.controlling.EventSystem;

import java.util.Vector;

public class JDBroadcaster {

    transient private Vector<JDListener> callList = null;

    transient private Vector<JDListener> removeList = null;

    public JDBroadcaster() {
        callList = new Vector<JDListener>();
        removeList = new Vector<JDListener>();
    }

    public void addJDListener(JDListener listener) {
        synchronized (callList) {
            synchronized (removeList) {
                if (removeList.contains(listener)) removeList.remove(listener);
            }
            if (!callList.contains(listener)) callList.add(listener);
        }
    }

    public boolean hasJDListener() {
        return callList.size() > 0;
    }

    public void fireJDEvent(JDEvent event) {
        synchronized (callList) {
            for (int i = callList.size() - 1; i >= 0; i--) {
                callList.get(i).receiveJDEvent(event);
            }
            synchronized (removeList) {
                callList.removeAll(removeList);
                removeList.clear();
            }
        }
    }

    public void removeJDListener(JDListener listener) {
        synchronized (removeList) {
            if (!removeList.contains(listener)) removeList.add(listener);
        }
    }
}

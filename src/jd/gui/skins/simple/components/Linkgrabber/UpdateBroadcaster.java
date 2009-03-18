package jd.gui.skins.simple.components.Linkgrabber;

import java.util.Vector;

public class UpdateBroadcaster {

    transient private Vector<UpdateListener> callList = null;

    transient private Vector<UpdateListener> removeList = null;

    protected UpdateBroadcaster() {
        callList = new Vector<UpdateListener>();
        removeList = new Vector<UpdateListener>();
    }

    public void addUpdateListener(UpdateListener listener) {
        synchronized (callList) {
            if (!callList.contains(listener)) callList.add(listener);
        }
    }

    public void fireUpdateEvent(UpdateEvent updateEvent) {
        synchronized (callList) {
            for (int i = callList.size() - 1; i >= 0; i--) {
                callList.get(i).UpdateEvent(updateEvent);
            }
            synchronized (removeList) {
                callList.removeAll(removeList);
                removeList.clear();
            }
        }
    }

    public void fireUpdateEvent(int controlID) {
        UpdateEvent c = new UpdateEvent(this, controlID);
        this.fireUpdateEvent(c);
    }

    public void fireUpdateEvent(int controlID, Object param) {
        UpdateEvent c = new UpdateEvent(this, controlID, param);
        this.fireUpdateEvent(c);
    }

    public void removeUpdateListener(UpdateListener listener) {
        synchronized (removeList) {
            if (!removeList.contains(listener)) removeList.add(listener);
        }
    }
}

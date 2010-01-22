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

package jd.event;

import java.util.EventListener;
import java.util.Vector;

import jd.controlling.JDLogger;

public abstract class JDBroadcaster<T extends EventListener, TT extends JDEvent> {

    transient protected Vector<T> callList = null;

    transient protected Vector<T> removeList = null;

    public JDBroadcaster() {
        callList = new Vector<T>();
        removeList = new Vector<T>();
    }

    public void addListener(final T listener) {
        if (removeList.contains(listener)) {
            removeList.remove(listener);
        }
        if (!callList.contains(listener)) {
            callList.add(listener);
        }
    }

    public boolean hasListener() {
        return callList.size() > 0;
    }

    public boolean fireEvent(final TT event) {
        synchronized (removeList) {
            callList.removeAll(removeList);
            removeList.clear();
        }
        for (int i = callList.size() - 1; i >= 0; i--) {
            try {
                this.fireEvent(callList.get(i), event);
            } catch (Exception e) {
                JDLogger.exception(e);
            }
        }
        return false;
    }

    protected abstract void fireEvent(T listener, TT event);

    public void removeListener(final T listener) {
        if (!removeList.contains(listener)) {
            removeList.add(listener);
        }
    }

    public Vector<T> getListener() {
        return callList;
    }

    public void addAllListener(final Vector<T> listener) {
        for (final T l : listener) {
            this.addListener(l);
        }
    }
}

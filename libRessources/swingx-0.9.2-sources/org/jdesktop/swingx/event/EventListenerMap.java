/**
 * Copyright 2006 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jdesktop.swingx.event;

import java.util.*;

/**
 * Intended to be a replacement for {@link javax.swing.event.EventListenerList}.
 *
 * @author Joshua Outwater
 */
public class EventListenerMap {
    private final Map<Class<? extends EventListener>, List<? extends EventListener>> listenerList =
            new HashMap<Class<? extends EventListener>, List<? extends EventListener>>();

    public List<EventListener> getListeners() {
        List<EventListener> listeners = new ArrayList<EventListener>();
        Set<Class<? extends EventListener>> keys = listenerList.keySet();
        for (Class<? extends EventListener> key : keys) {
            listeners.addAll(listenerList.get(key));
        }

        return listeners;
    }

    public <T extends EventListener> List<T> getListeners(Class<T> clazz) {
        //noinspection unchecked
        List<T> list = (List<T>) listenerList.get(clazz);
        if (list == null) {
            list = new ArrayList<T>();
        }
        return list;
    }

    public int getListenerCount() {
        int count = 0;
        Set<Class<? extends EventListener>> keys = listenerList.keySet();
        for (Class<? extends EventListener> key : keys) {
            count += listenerList.get(key).size();
        }
        return count;
    }

    public <T extends EventListener> int getListenerCount(Class<T> clazz) {
        //noinspection unchecked
        List<T> list = (List<T>) listenerList.get(clazz);
        if (list != null) {
            return list.size();
        }
        return 0;
    }

    public <T extends EventListener> void add(Class<T> clazz, T listener) {
        if (listener == null) {
            return;
        }

        //noinspection unchecked
        List<T> list = (List<T>) listenerList.get(clazz);
        if (list == null) {
            list = new ArrayList<T>();
            listenerList.put(clazz, list);
        }
        list.add(listener);
    }

    public <T extends EventListener> void remove(Class<T> clazz, T listener) {
        if (listener == null) {
            return;
        }

        //noinspection unchecked
        List<T> list = (List<T>) listenerList.get(clazz);
        if (list != null) {
            list.remove(listener);
        }
    }
}
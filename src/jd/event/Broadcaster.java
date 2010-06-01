//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.util.ArrayList;

/**
 * TODO: Use {@link JDBroadcaster} instead. Theres no need for two differend
 * broadcasters.
 */
public class Broadcaster<E> {

    private ArrayList<E> listener;

    public Broadcaster() {
        listener = new ArrayList<E>();
    }

    public synchronized void addListener(E listener) {
        this.listener.add(listener);
    }

    public synchronized void removeListener(E listener) {
        this.listener.remove(listener);
    }

    public int size() {
        return listener.size();
    }

    public synchronized E get(int i) {
        return listener.get(i);
    }

}

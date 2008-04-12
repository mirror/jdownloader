//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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


package jd.controlling;

import java.util.Iterator;
import java.util.Vector;

import jd.event.ControlEvent;
import jd.event.ControlListener;


/**
 * Diese Klasse  
 * 
 * @author astaldo
 */
public class ControlBroadcaster extends Thread {
    /**
     * Hiermit wird der Eventmechanismus realisiert. Alle hier eingetragenen Listener
     * werden benachrichtigt, wenn mittels {@link #firePluginEvent(PluginEvent)} ein
     * Event losgeschickt wird.
     */
    private Vector<ControlListener> controlListener = null;
    protected ControlBroadcaster(String name) {
        super(name);
        controlListener = new Vector<ControlListener>();
    }
    /**
     * Fügt einen Listener hinzu
     * 
     * @param listener Ein neuer Listener
     */
    public void addControlListener(ControlListener listener) {
        if (controlListener.indexOf(listener) == -1) {
            controlListener.add(listener);
        }
    }
    /**
     * Emtfernt einen Listener
     * @param listener Der zu entfernende Listener
     */
    public void removeControlListener(ControlListener listener) {
        controlListener.remove(listener);
    }
    /**
     * Verteilt Ein Event an alle Listener
     * 
     * @param controlEvent ein abzuschickendes Event
     */
    public void fireControlEvent(ControlEvent controlEvent) {
        Iterator<ControlListener> iterator = controlListener.iterator();
        while (iterator.hasNext()) {
             ((ControlListener) iterator.next()).controlEvent(controlEvent);
        }
    }
    
    public void fireControlEvent(int controlID,Object param) {
        ControlEvent c = new ControlEvent(this,controlID,param);
        Iterator<ControlListener> iterator = controlListener.iterator();
        while (iterator.hasNext()) {
             ((ControlListener) iterator.next()).controlEvent(c);
        }
    }
}

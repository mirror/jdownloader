package jd.controlling;

import java.util.Iterator;
import java.util.Vector;

import jd.controlling.event.ControlEvent;
import jd.controlling.event.ControlListener;
import jd.plugins.event.PluginEvent;
/**
 * Diese Klasse  
 * 
 * @author astaldo
 */
public class ControlMulticaster extends Thread{
    /**
     * Hiermit wird der Eventmechanismus realisiert. Alle hier eingetragenen Listener
     * werden benachrichtigt, wenn mittels {@link #firePluginEvent(PluginEvent)} ein
     * Event losgeschickt wird.
     */
    private Vector<ControlListener> controlListener = null;
    protected ControlMulticaster(String name){
        super(name);
        controlListener = new Vector<ControlListener>();
    }
    /**
     * Fügt einen Listener hinzu
     * 
     * @param listener Ein neuer Listener
     */
    public void addControlListener(ControlListener listener) {
        controlListener.add(listener);
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

        while(iterator.hasNext()) {
            ((ControlListener)iterator.next()).controlEvent(controlEvent);
        }
    }

}

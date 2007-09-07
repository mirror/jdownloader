package jd.controlling.interaction;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.plugins.Plugin;
import jd.plugins.event.PluginEvent;

/**
 * Mit dieser Klasse werden Interaktionen (mit dem System) umgesetzt
 * 
 * @author astaldo
 */
public abstract class Interaction implements Serializable {
    /**
     * serialVersionUID
     */
    private transient static final long serialVersionUID                   = -5609631258725998799L;

    protected transient static Logger   logger                             = Plugin.getLogger();
/**
 * Gibt an ob die INteraction den Thread gestartet hat
 */
    protected boolean                   threadStarted                      = false;
   /**
     * THread der für die INteraction verwendet werden kann
     */
    
    protected Thread thread = null;
    /**
     * Hiermit wird der Eventmechanismus realisiert. Alle hier eingetragenen
     * Listener werden benachrichtigt, wenn mittels
     * {@link #firePluginEvent(PluginEvent)} ein Event losgeschickt wird.
     */
    private Vector<ControlListener>     controlListener                    = null;

    /**
     * Code der abgerufe werden kann um details über den Ablauf der Interaction
     * zu kriegen
     */
    protected int                       lastCallCode                       = 0;

    /**
     * Zeigt an dass diese Interaction noch nie aufgerufen wurde
     */
    public transient final static int   INTERACTION_CALL_NEVERCALLED       = 0;

    /**
     * Zeigt an dass die INteraction erfolgreioch beendet wurde
     */
    public transient final static int   INTERACTION_CALL_SUCCESS           = 1;

    /**
     * Zeigt an dass die Interaction mit Fehlern beendet wurde
     */
    public transient final static int   INTERACTION_CALL_ERROR             = 2;

    /**
     * Zeigt dass die INteraction gerade läuft
     */
    public transient final static int   INTERACTION_CALL_RUNNING           = 3;

    // Download IDS
    /**
     * Zeigt an, daß ein einzelner Download beendet wurde
     */
    public transient final static int   INTERACTION_DOWNLOAD_FINISHED      = 1;

    /**
     * Zeigt an, daß alle Downloads abgeschlossen wurden
     */
    public transient final static int   INTERACTION_DOWNLOADS_FINISHED_ALL = 2;

    /**
     * Zeigt, daß ein einzelner Download nicht fertiggestellt werden konnte
     */
    public transient final static int   INTERACTION_DOWNLOAD_FAILED        = 3;

    /**
     * Zeigt, daß ein einzelner Download wegen Wartezeit nicht starten konnte
     */
    public transient final static int   INTERACTION_DOWNLOAD_WAITTIME      = 4;

    /**
     * Zeigt, daß ein der Bot erkannt wurde
     */
    public transient final static int   INTERACTION_DOWNLOAD_BOT_DETECTED  = 5;

    /**
     * Zeigt, daß ein Captcha erkannt werden will
     */
    public transient final static int   INTERACTION_DOWNLOAD_CAPTCHA       = 6;

    /**
     * Zeigt den Programmstart an
     */
    public transient final static int   INTERACTION_APPSTART               = 7;

    /**
     * Zeigt den Programmende an
     */
    public transient final static int   INTERACTION_APPTERMINATE           = 8;

    /**
     * Zeigt, dass vermutlich JAC veraltet ist
     */
    public transient final static int   INTERACTION_JAC_UPDATE_NEEDED      = 9;

    public Interaction() {
        controlListener = new Vector<ControlListener>();
    }

    public abstract boolean doInteraction(Object arg);

    public abstract String toString();

    public abstract String getInteractionName();
/**
 * Thread Funktion. Diese Funktion wird aufgerufen wenn INteraction.start()
 * aufgerufen wird. Dabei wird ein neuer thread erstellt
 */
    public abstract void run();

    /**
     * Gibt den callcode zurück. Dieser gibt Aufschlussdarüber wie die
     * INteraction abgelaufen ist
     * 
     * @return callcode
     */
    public int getCallCode() {
        return lastCallCode;
    }

    /**
     * ruft die doInteraction Funktion auf. Und setzt das Ergebnis als callCode.
     * Der Statuscode kann mit getCallCode abgerufen werden
     * 
     * @param arg
     * @return
     */
    public boolean interact(Object arg) {
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_INTERACTION_ACTIVE, this));
        this.setCallCode(Interaction.INTERACTION_CALL_RUNNING);
        boolean success = doInteraction(arg);
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_INTERACTION_RETURNED, this));
        if (!this.isAlive()) {
          
            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_INTERACTION_INACTIVE, this));
        }
        return success;
    }

    /**
     * Setzt den callCode
     * 
     * @param callCode
     */
    public void setCallCode(int callCode) {
        this.lastCallCode = callCode;
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
     * 
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
/**
 * Wird vom neuen Thread aufgerufen, setzt die ThreadVariable
 */
    private void runThreadAction() {       
        this.run();     
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_INTERACTION_INACTIVE, this));
    }
/**
 * Gibt an ob der Thread Aktiv ist
 * 
 * @return
 */
    public boolean isAlive() {
        if(thread==null)return false;
        return thread.isAlive();
    }

/**
 * Erstellt einen neuen Thread und führt den zugehörigen Code aus (run()
 */
    protected void start() {
        final Interaction _this = this;
        thread= new Thread() {
            public void run() {
                _this.runThreadAction();
            }

        };
        thread.start();

    }

/**
 * Führt die Interactionen aus
 * @param localInteractions Interactionen
 * @param controller Aktueller Kontroller
 * @param param Parameter für die Interactionen
 * @return
 */
    public static boolean handleInteraction(Vector<Interaction> localInteractions, JDController controller, Object param) {
        boolean ret = true;       
        if (localInteractions != null && localInteractions.size() > 0) {
            Iterator<Interaction> iterator = localInteractions.iterator();

            while (iterator.hasNext()) {
                Interaction i = iterator.next();
                i.addControlListener(controller);

                if (!i.interact(param)) {
                    ret = false;
                    logger.severe("interaction failed: " + i);
                }
                else {
                    logger.info("interaction successfull: " + i);
                }

            }
        }
        else {
            return false;
        }

        return ret;

    }
}

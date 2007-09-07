package jd.controlling.interaction;

import java.io.Serializable;
import java.util.logging.Logger;

import jd.plugins.Plugin;

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
    public transient final static int   INTERACTION_APPSTART       = 7;
    
    /**
     * Zeigt den Programmende an
     */
    public transient final static int   INTERACTION_APPTERMINATE      = 8;
    
    /**
     * Zeigt, dass vermutlich JAC veraltet ist 
     */
    public transient final static int   INTERACTION_JAC_UPDATE_NEEDED       = 9;

    public Interaction() {
       
    }

    public abstract boolean doInteraction(Object arg);

    public abstract String toString();

    public abstract String getName();

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
     * ruft die doInteraction Funktion auf. Und setzt das Ergebnis als callCode. Der Statuscode kann mit getCallCode abgerufen werden
     * @param arg
     * @return
     */
    public boolean interact(Object arg) {
        
        boolean success = doInteraction(arg);
        if (this.getCallCode() == Interaction.INTERACTION_CALL_NEVERCALLED) this.setCallCode(Interaction.INTERACTION_CALL_SUCCESS);
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
}

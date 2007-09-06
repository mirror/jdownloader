package jd.controlling.interaction;

import java.io.Serializable;
import java.util.logging.Logger;

import jd.plugins.Plugin;
/**
 * Mit dieser Klasse werden Interaktionen (mit dem System) umgesetzt
 * 
 * @author astaldo
 */
public abstract class Interaction implements Serializable{
    /**
     * serialVersionUID
     */
    private   transient static final long serialVersionUID = -5609631258725998799L;
    protected transient static Logger logger = Plugin.getLogger();
    /**
     * Zeigt an, daß ein einzelner Download beendet wurde
     */
    public transient final static int INTERACTION_DOWNLOAD_FINISHED      = 1;
    /**
     * Zeigt an, daß alle Downloads abgeschlossen wurden
     */
    public transient final static int INTERACTION_DOWNLOADS_FINISHED_ALL = 2;
    /**
     * Zeigt, daß ein einzelner Download nicht fertiggestellt werden konnte
     */
    public transient final static int INTERACTION_DOWNLOAD_FAILED        = 3;
    /**
     * Zeigt, daß ein einzelner Download wegen Wartezeit nicht starten konnte
     */
    public transient final static int INTERACTION_DOWNLOAD_WAITTIME       = 4;
    /**
     * Zeigt, daß ein der Bot erkannt wurde
     */
    public transient final static int INTERACTION_DOWNLOAD_BOT_DETECTED      = 5;
    /**
     * Zeigt, daß ein Captcha erkannt werden will
     */
    public transient final static int INTERACTION_DOWNLOAD_CAPTCHA      = 6;
    public Interaction(){
    }

    public abstract boolean interact(Object arg); 
    public abstract String toString();
    public abstract String getName();
}

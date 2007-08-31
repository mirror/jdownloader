package jd.controlling.interaction;

import java.io.Serializable;
import java.util.logging.Logger;

import jd.Configuration;
import jd.JDUtilities;
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

    public static Configuration configuration;
    
    public Interaction(){
        configuration = JDUtilities.getConfiguration();
    }

    public abstract boolean interact(); 
    public abstract String toString();
    public abstract String getName();
}

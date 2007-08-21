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
    
    public transient final static int INTERACTION_DOWNLOAD_FINISHED      = 1;
    public transient final static int INTERACTION_ALL_DOWNLOADS_FINISHED = 2;
    public transient final static int INTERACTION_DOWNLOAD_FAILED        = 3;

    public static Configuration configuration = JDUtilities.getConfiguration();

    public abstract boolean interact(); 
    public abstract String toString();
}

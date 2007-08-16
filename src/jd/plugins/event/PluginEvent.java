package jd.plugins.event;

import java.awt.AWTEvent;

import jd.plugins.Plugin;
/**
 * Mit diesen Events kommunizieren die Plugins mit dem Hauptprogramm
 *
 * @author astaldo
 */
public class PluginEvent extends AWTEvent{
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -7243557276278230057L;

    public static final int PLUGIN_PROGRESS_MAX          = 1;
    public static final int PLUGIN_PROGRESS_INCREASE     = 2;
    public static final int PLUGIN_PROGRESS_VALUE        = 3;
    public static final int PLUGIN_PROGRESS_FINISH       = 4;
    public static final int PLUGIN_CRYPT_LINKS_DECRYPTED = 5;
    public static final int PLUGIN_DATA_CHANGED          = 6;
    /**
     * Plugin, von dem dieses Event ausgegangen ist
     */
    private Plugin source;
    /**
     * ID des Events
     */
    private int    eventID;
    /**
     * Optionaler Parameter
     */
    private Object parameter;

    public PluginEvent(Plugin source, int eventID, Object parameter){
        super(source,eventID);
        this.source =source;
        this.eventID = eventID;
        this.parameter = parameter;
    }
    public int getEventID()       { return eventID;   }
    public Plugin getSource()     { return source;    }
    public Object getParameter1() { return parameter; }
}

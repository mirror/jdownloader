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
    /**
     * Maximaler Wert für die Fortschrittsanzeige
     */
    public static final int PLUGIN_PROGRESS_MAX          = 1;
    /**
     * Der Wert der Fortschrittsanzeige soll um eins erhöht werden
     */
    public static final int PLUGIN_PROGRESS_INCREASE     = 2;
    /**
     * Der Wert der Fortschrittsanzeige soll auf diesen Wert gesetzt werden
     */
    public static final int PLUGIN_PROGRESS_VALUE        = 3;
    /**
     * Die Vorgang ist beendet
     */
    public static final int PLUGIN_PROGRESS_FINISH       = 4;
    /**
     * Links wurden entschlüsselt
     */
    public static final int PLUGIN_CRYPT_LINKS_DECRYPTED = 5;
    /**
     * Daten des Plugins haben sich geändert
     */
    public static final int PLUGIN_DATA_CHANGED          = 6;
    /**
     * Download Geschwindigkeit hat sich geÃ¤ndert
     */
    public static final int PLUGIN_DOWNLOAD_SPEED        = 7;
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
    /**
     * Erstellt ein neues PluginEvent
     * 
     * @param source Das Plugin, daß dieses Event ausgelöst hat
     * @param eventID Die ID des Events
     * @param parameter Ein optionaler Parameter
     */
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

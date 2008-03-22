//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://wnu.org/licenses/>.


package jd.plugins.event;

import java.awt.AWTEvent;

import jd.plugins.Plugin;

/**
 * Mit diesen Events kommunizieren die Plugins mit dem Hauptprogramm
 * 
 * @author astaldo
 */
public class PluginEvent extends AWTEvent {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID             = -7243557276278230057L;
    /**
     * Neue Bytes wurden geladen bytes anzahl als parameter
     */
    //public static final int   PLUGIN_DOWNLOAD_BYTES        = 0;
   
    /**
     * Links wurden entschlüsselt
     */
    public static final int   PLUGIN_CRYPT_LINKS_DECRYPTED = 5;
    /**
     * Daten des Plugins haben sich geändert
     */
    public static final int   PLUGIN_DATA_CHANGED          = 6;
    /**
     * Download Geschwindigkeit hat sich geändert
     */
    public static final int   PLUGIN_DOWNLOAD_SPEED        = 7;
    /**
     * Die UI soll angezeigt werden
     */
    public static final int   PLUGIN_CONTROL_SHOW_UI       = 8;
    /**
     * Die Konfiguration soll angezeigt werden
     */
    public static final int   PLUGIN_CONTROL_SHOW_CONFIG   = 9;
    /**
     * Drag & Drop soll aktiviert werden
     */
    public static final int   PLUGIN_CONTROL_DND           = 10;
    /**
     * JDownloader soll beendet werden
     */
    public static final int   PLUGIN_CONTROL_EXIT          = 11;
    /**
     * Die Downloads sollen gestartet/gestoppt werden
     */
    public static final int   PLUGIN_CONTROL_START_STOP    = 12;
    /**
     * Die Verbindung sol getrennt werden
     */
    public static final int   PLUGIN_CONTROL_RECONNECT     = 13;
    /**
     * Plugin, von dem dieses Event ausgegangen ist
     */
    private Plugin            source;
    /**
     * ID des Events
     */
    private int               eventID;
    /**
     * Optionaler Parameter
     */
    private Object            parameter;
    /**
     * Erstellt ein neues PluginEvent
     * 
     * @param source Das Plugin, daß dieses Event ausgelöst hat
     * @param eventID Die ID des Events
     * @param parameter Ein optionaler Parameter
     */
    public PluginEvent(Plugin source, int eventID, Object parameter) {
        super(source, eventID);
        this.source = source;
        this.eventID = eventID;
        this.parameter = parameter;
    }
    
    public int getEventID() {
        return eventID;
    }
    public Plugin getSource() {
        return source;
    }
    // Hat das einen grund warumd as getParameter1 heißt?
    public Object getParameter1() {
        return parameter;
    }
}

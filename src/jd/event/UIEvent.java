package jd.event;

import java.awt.AWTEvent;

import jd.gui.UIInterface;

public class UIEvent extends AWTEvent{
    
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -5178146758475854235L;
    
    private UIInterface uiInterface;
    private int actionID;
    private Object parameter;
    /**
     * Die DownloadLinks wurden verändert (zB in der Reihenfolge)
     */
    public static final int UI_LINKS_CHANGED    = 1;
    /**
     * Der Download sol gestartet werden
     */
    public static final int UI_START_DOWNLOADS  = 2;
    /**
     * Der Download soll angehalten werden
     */
    public static final int UI_STOP_DOWNLOADS   = 3;
    /**
     * Alle Links sollen geladen werden
     */
    public static final int UI_LOAD_LINKS       = 4;
    /**
     * Alle Links sollen gespeichert werden
     */
    public static final int UI_SAVE_LINKS       = 5;
    /**
     * Es sollen Daten überprüft und ggf als DownloadLinks hinzugefügt werden
     */
    public static final int UI_LINKS_TO_PROCESS = 6;
    /**
     * Die Konfiguration soll gespeichtert werden
     */
    public static final int UI_SAVE_CONFIG = 7;
    /**
     * Ein Update soll durchgeführt werden
     */
    public static final int UI_INTERACT_UPDATE = 9;
    /**
     * Ein Reconnect soll gemacht werden
     */
    public static final int UI_INTERACT_RECONNECT = 10;
    /**
     * Die Anwendung soll geschlossen werden
     */
    public static final int UI_EXIT = 11;
    
    /**
     * DragAndDrop Event
     */
        public static final int UI_DRAG_AND_DROP = 12;
/**
 * Clipboard an/aus   boolean als parameter
 */
    public static final int UI_SET_CLIPBOARD = 13;
/**
 * Der Linkgrabber hat Links zurückgegeben
 */
public static final int UI_LINKS_GRABBED = 14; 
    public UIEvent(UIInterface uiInterface, int actionID){
        this(uiInterface,actionID,null);
    }
    
    public UIEvent(Object uiInterface, int actionID, Object parameter){
        super(uiInterface, actionID);
        if(uiInterface instanceof UIInterface){
        this.uiInterface = (UIInterface)uiInterface;
        }
        this.actionID = actionID;
        this.parameter = parameter;
    }

    public UIInterface getUiInterface() { return uiInterface; }
    public int getActionID()            { return actionID;    }
    public Object getParameter()        { return parameter;   }
    
}

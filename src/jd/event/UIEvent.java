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
    
    public UIEvent(UIInterface uiInterface, int actionID){
        this(uiInterface,actionID,null);
    }
    public UIEvent(UIInterface uiInterface, int actionID, Object parameter){
        super(uiInterface, actionID);
        this.uiInterface = uiInterface;
        this.actionID = actionID;
        this.parameter = parameter;
    }

    public UIInterface getUiInterface() { return uiInterface; }
    public int getActionID()            { return actionID;    }
    public Object getParameter()        { return parameter;   }
    
}

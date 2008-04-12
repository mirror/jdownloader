//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


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
    public static final int UI_UPDATED_LINKLIST      = 1;
    /**
     * Der Download sol gestartet werden
     */
    public static final int UI_START_DOWNLOADS    = 2;
    /**
     * Der Download soll angehalten werden
     */
    public static final int UI_STOP_DOWNLOADS     = 3;
    /**
     * Alle Links sollen geladen werden
     */
    public static final int UI_LOAD_LINKS         = 4;
    /**
     * Alle Links sollen gespeichert werden
     */
    public static final int UI_SAVE_LINKS         = 5;
    /**
     * Es sollen Daten überprüft und ggf als DownloadLinks hinzugefügt werden
     */
    public static final int UI_LINKS_TO_PROCESS   = 6;
    /**
     * Die Konfiguration soll gespeichtert werden
     */
    //public static final int UI_SAVE_CONFIG        = 7;
    /**
     * Ein Update soll durchgeführt werden
     */
    public static final int UI_INTERACT_UPDATE    = 9;
    /**
     * Ein Reconnect soll gemacht werden
     */
    public static final int UI_INTERACT_RECONNECT = 10;
    /**
     * Die Anwendung soll geschlossen werden
     */
    public static final int UI_EXIT               = 11;
    /**
     * DragAndDrop Event
     */
    public static final int UI_DRAG_AND_DROP      = 12;

    /**
     * Der Linkgrabber hat Links zurückgegeben
     */
    public static final int UI_PACKAGE_GRABBED      = 14;
    /**
     * Eine Containerdatei soll geladen werden
     */
  //  public static final int UI_LOAD_CONTAINER     = 15;

    public static final int UI_PAUSE_DOWNLOADS = 16;

    public static final int   UI_SET_STARTSTOP_BUTTON_STATE = 17;

    public static final int   UI_SET_MINIMIZED              = 18;

    
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

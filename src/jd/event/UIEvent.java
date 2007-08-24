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
    
    public static final int UI_LINKS_CHANGED   = 1;
    public static final int UI_START_DOWNLOADS = 2;
    public static final int UI_STOP_DOWNLOADS  = 3;
    public static final int UI_LOAD_LINKS      = 4;
    public static final int UI_SAVE_LINKS      = 5;
    public static final int UI_LINKS_TO_PROCESS    = 6;
    
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

package jd.gui.swing.jdgui.interfaces;

import jd.event.JDEvent;
/**
 * 
 * @author Coalado
 *
 */
public class SwitchPanelEvent extends JDEvent {

 
/**
 * panel is noe visible on screen
 */
    public static final int ON_SHOW = 0;
    /**
     * Panel is not visible any more
     */
    public static final int ON_HIDE = 1;
    /**
     * panel has been added to the gui. this does NOT mean that it is visible.
     * e.g. it may be a tab of a tabbedpane, but not the selected one
     */
    public static final int ON_ADD = 2;
    
    /**
     * Panel has been removed from gui. see SwitchPanelEvent.ON_ADD
     */
    public static final int ON_REMOVE = 3;
    public SwitchPanelEvent(Object source, int ID, Object parameter) {
        super(source, ID,parameter);
     
       
    }

    public SwitchPanelEvent(Object source, int ID) {
      this(source,ID,null);
    }


}
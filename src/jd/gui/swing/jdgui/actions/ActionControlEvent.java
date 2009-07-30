package jd.gui.swing.jdgui.actions;

import jd.event.JDEvent;

public class ActionControlEvent extends JDEvent {

    public static final int PROPERTY_CHANGED = 0;

    public ActionControlEvent(Object source, int ID) {
        this(source, ID,null);
       
    }

    public ActionControlEvent(Object source, int ID, String propertyName) {
        super(source, ID, propertyName);
    }

}

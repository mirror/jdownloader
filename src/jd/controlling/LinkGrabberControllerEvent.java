package jd.controlling;

import jd.event.JDEvent;

public class LinkGrabberControllerEvent extends JDEvent {
    public LinkGrabberControllerEvent(Object source, int ID) {
        super(source, ID);
    }

    public LinkGrabberControllerEvent(Object source, int ID, Object param) {
        super(source, ID, param);
    }

    public static final int REFRESH_STRUCTURE = 1;

    public static final int ADD_FILEPACKAGE = 2;

    public static final int REMOVE_FILPACKAGE = 3;
    
    public static final int ADDED = 5;

    public static final int EMPTY = 4;
}

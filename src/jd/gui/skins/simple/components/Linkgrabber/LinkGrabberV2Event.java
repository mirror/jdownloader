package jd.gui.skins.simple.components.Linkgrabber;

import jd.controlling.EventSystem.JDEvent;

public class LinkGrabberV2Event extends JDEvent {

    public LinkGrabberV2Event(Object source, int ID) {
        super(source, ID);
        // TODO Auto-generated constructor stub
    }

    /* die Links im Linkgrabber wurden geändert */
    public static final int UPDATE_EVENT = 1;

    /* keine Links im Linkgrabber */
    public static final int EMPTY_EVENT = 999;

}

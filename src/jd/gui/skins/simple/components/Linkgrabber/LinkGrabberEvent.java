package jd.gui.skins.simple.components.Linkgrabber;

import jd.event.JDEvent;

public class LinkGrabberEvent extends JDEvent {

    public LinkGrabberEvent(Object source, int ID) {
        super(source, ID);
        // TODO Auto-generated constructor stub
    }

    /* die Links im Linkgrabber wurden geändert */
    public static final int UPDATE_EVENT = 1;

    /* keine Links im Linkgrabber */
    public static final int EMPTY_EVENT = 999;

}

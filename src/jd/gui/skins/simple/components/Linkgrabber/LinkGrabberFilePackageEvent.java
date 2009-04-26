package jd.gui.skins.simple.components.Linkgrabber;

import jd.event.JDEvent;

public class LinkGrabberFilePackageEvent extends JDEvent {

    public LinkGrabberFilePackageEvent(Object source, int ID) {
        super(source, ID);
        // TODO Auto-generated constructor stub
    }

    /* ein wichtiger Wert wurde geändert */
    public static final int UPDATE_EVENT = 1;

    public static final int ADD_LINK = 2;
    public static final int REMOVE_LINK = 3;

    /* das FilePackage ist leer */
    public static final int EMPTY_EVENT = 999;

}

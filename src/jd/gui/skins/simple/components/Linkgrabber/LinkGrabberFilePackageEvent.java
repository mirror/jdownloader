package jd.gui.skins.simple.components.Linkgrabber;

import jd.event.JDEvent;

public class LinkGrabberFilePackageEvent extends JDEvent {

    public LinkGrabberFilePackageEvent(Object source, int ID) {
        super(source, ID);
        // TODO Auto-generated constructor stub
    }

    /* ein wichtiger Wert wurde geändert */
    public static final int UPDATE_EVENT = 1;

    /* das FilePackage ist leer */
    public static final int EMPTY_EVENT = 999;

}

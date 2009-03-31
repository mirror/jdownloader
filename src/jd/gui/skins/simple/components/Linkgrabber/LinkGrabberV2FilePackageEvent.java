package jd.gui.skins.simple.components.Linkgrabber;

import jd.controlling.EventSystem.JDEvent;

public class LinkGrabberV2FilePackageEvent extends JDEvent {

    public LinkGrabberV2FilePackageEvent(Object source, int ID) {
        super(source, ID);
        // TODO Auto-generated constructor stub
    }

    /* ein wichtiger Wert wurde geändert */
    public static final int UPDATE_EVENT = 1;

    /* das FilePackage ist leer */
    public static final int EMPTY_EVENT = 999;

}

package jd.gui.skins.simple.components.Linkgrabber;

import jd.event.JDEvent;

public class LinkCheckEvent extends JDEvent {

    public LinkCheckEvent(Object source, int ID, Object parameter) {
        super(source, ID, parameter);
        // TODO Auto-generated constructor stub
    }

    public LinkCheckEvent(Object source, int ID) {
        super(source, ID);
        // TODO Auto-generated constructor stub
    }

    public final static int START = 1;
    public final static int STOP = 2;
    public final static int ABORT = 3;
    public final static int AFTER_CHECK = 4;
}

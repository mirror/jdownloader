package jd.controlling;

import jd.event.JDEvent;

public class JDDownloadControllerEvent extends JDEvent {

    public JDDownloadControllerEvent(Object source, int ID, Object parameter) {
        super(source, ID, parameter);
    }

    public JDDownloadControllerEvent(Object source, int ID) {
        super(source, ID);
    }

    public static final int UPDATE = 1;
    public static final int REFRESH = 2;
}

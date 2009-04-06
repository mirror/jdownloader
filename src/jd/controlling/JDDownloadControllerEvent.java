package jd.controlling;

import jd.event.JDEvent;

public class JDDownloadControllerEvent extends JDEvent {

    public JDDownloadControllerEvent(Object source, int ID, Object parameter) {
        super(source, ID, parameter);
    }

    public JDDownloadControllerEvent(Object source, int ID) {
        super(source, ID);
    }

    /**
     * Wird bei Strukturänderungen der DownloadListe
     */
    public static final int UPDATE = 1;

    public static final int ADD_FP = 2;

    public static final int REMOVE_FP = 3;

}

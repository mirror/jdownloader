package jd.controlling;

import jd.event.JDEvent;

public class DownloadControllerEvent extends JDEvent {
    public DownloadControllerEvent(Object source, int ID) {
        super(source, ID);
    }

    public DownloadControllerEvent(Object source, int ID, Object param) {
        super(source, ID, param);
    }

    /**
     * Wird bei Strukturänderungen der DownloadListe
     */
    public static final int REFRESH_STRUCTURE = 1;

    /* Downloadlink oder ArrayList<DownloadLink> soll aktuallisiert werden */
    public static final int REFRESH_SPECIFIC = 11;

    /* die komplette liste soll aktuallisiert werden */
    public static final int REFRESH_ALL = 12;

    public static final int ADD_FILEPACKAGE = 2;

    public static final int REMOVE_FILPACKAGE = 3;

    public static final int ADD_DOWNLOADLINK = 4;

    public static final int REMOVE_DOWNLOADLINK = 5;
}

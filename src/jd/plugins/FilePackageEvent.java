package jd.plugins;

import jd.event.JDEvent;

public class FilePackageEvent extends JDEvent {

    /* ein wichtiger Wert wurde geändert */
    public static final int FILEPACKAGE_UPDATE = 1;

    public static final int DOWNLOADLINK_ADDED = 2;
    public static final int DOWNLOADLINK_REMOVED = 3;

    /* das FilePackage ist leer */
    public static final int FILEPACKAGE_EMPTY = 999;

    public FilePackageEvent(Object source, int ID) {
        super(source, ID);
        // TODO Auto-generated constructor stub
    }

    public FilePackageEvent(Object source, int ID, Object param) {
        super(source, ID, param);
        // TODO Auto-generated constructor stub
    }

}

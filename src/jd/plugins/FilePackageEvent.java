package jd.plugins;

import jd.event.JDEvent;

public class FilePackageEvent extends JDEvent {

    /* ein wichtiger Wert wurde geändert */
    public static final int FP_UPDATE = 1;

    public static final int DL_ADDED = 2;
    public static final int DL_REMOVED = 3;

    /* das FilePackage ist leer */
    public static final int FP_EMPTY = 999;

    public FilePackageEvent(Object source, int ID) {
        super(source, ID);
        // TODO Auto-generated constructor stub
    }

}

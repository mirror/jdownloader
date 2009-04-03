package jd.plugins;

import jd.event.JDEvent;

public class FilePackageEvent extends JDEvent {

    /* ein wichtiger Wert wurde geändert */
    public static final int UPDATE_EVENT = 1;

    /* das FilePackage ist leer */
    public static final int EMPTY_EVENT = 999;

    public FilePackageEvent(Object source, int ID, Object parameter) {
        super(source, ID, parameter);
        // TODO Auto-generated constructor stub
    }

    public FilePackageEvent(Object source, int ID) {
        super(source, ID);
        // TODO Auto-generated constructor stub
    }

}

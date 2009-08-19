package jd.utils.locale;

import jd.event.JDEvent;

public class JDLEvent extends JDEvent {
    /**
     * a new languagefile ahas been loaded. maybe their are new setttings, too
     * parameter: JDLocale instance
     */
    public static final int SET_NEW_LOCALE = 1;
    public JDLEvent(Object source, int ID) {
        super(source, ID);
       
    }

}

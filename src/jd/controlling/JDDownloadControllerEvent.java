package jd.controlling;

import jd.event.JDEvent;

public class JDDownloadControllerEvent extends JDEvent {

    public JDDownloadControllerEvent(Object source, int ID, Object parameter) {
        super(source, ID, parameter);
        // TODO Auto-generated constructor stub
    }

    public JDDownloadControllerEvent(Object source, int ID) {
        super(source, ID);
        // TODO Auto-generated constructor stub
    }
    
    public static final int UPDATE=1;
    public static final int REFRESH=2;
}

package jd.controlling;

import jd.event.JDEvent;

public class ProgressControllerEvent extends JDEvent {

    public ProgressControllerEvent(Object source, int ID) {
        super(source, ID);
        // TODO Auto-generated constructor stub
    }

    public static final int CANCEL = 1;

}

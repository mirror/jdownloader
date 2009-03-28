package jd.controlling;

import jd.controlling.EventSystem.JDEvent;

public class ProgressControllerEvent extends JDEvent {

    public static final int CANCEL = 1;

    public ProgressControllerEvent(Object source, int ID) {
        super(source, ID);
        // TODO Auto-generated constructor stub
    }

}

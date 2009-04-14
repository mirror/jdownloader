package jd.controlling;

import jd.event.JDEvent;

public class AccountsUpdateEvent extends JDEvent {

    public static final int CHANGED = 0;

    public AccountsUpdateEvent(Object source, int ID) {
        super(source, ID);
        // TODO Auto-generated constructor stub
    }

}

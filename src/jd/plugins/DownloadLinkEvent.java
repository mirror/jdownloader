package jd.plugins;

import jd.event.JDEvent;

public class DownloadLinkEvent extends JDEvent {

    public DownloadLinkEvent(Object source, int ID) {
        super(source, ID);
        // TODO Auto-generated constructor stub
    }

    public static final int UPDATE = 1;

    public static final int UPDATE_LOADING_PROGRESS = 2;

    public static final int ENABLED = 3;

    public static final int DISABLED = 4;
}

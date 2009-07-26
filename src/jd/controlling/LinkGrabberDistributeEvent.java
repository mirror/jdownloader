package jd.controlling;

import java.util.ArrayList;

import jd.plugins.DownloadLink;

public interface LinkGrabberDistributeEvent {
    public void addLinks(ArrayList<DownloadLink> links, boolean hidegrabber, boolean autostart);

}

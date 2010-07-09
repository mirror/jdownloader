package jd.http.ext.events;

import jd.http.ext.ExtBrowser;

import org.appwork.utils.event.Event;

public class ExtBrowserEvent extends Event {

    private ExtBrowser browser;

    public ExtBrowser getBrowser() {
        return browser;
    }

    public ExtBrowserEvent(ExtBrowser browser, Object caller, int eventID, Object parameter) {
        super(caller, eventID, parameter);
        this.browser = browser;
        // TODO Auto-generated constructor stub
    }

}

package jd.http.ext.events;

import jd.http.ext.ExtBrowser;

import org.appwork.utils.event.DefaultEvent;

public class ExtBrowserEvent extends DefaultEvent {

    private final ExtBrowser browser;

    public ExtBrowserEvent(ExtBrowser browser, Object caller, int eventID, Object parameter) {
        super(caller, eventID, parameter);
        this.browser = browser;
    }

    public ExtBrowser getBrowser() {
        return browser;
    }

}

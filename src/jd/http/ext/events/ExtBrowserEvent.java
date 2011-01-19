package jd.http.ext.events;

import jd.http.ext.ExtBrowser;

public class ExtBrowserEvent extends DefaultIntEvent {

    private final ExtBrowser browser;

    public ExtBrowserEvent(ExtBrowser browser, Object caller, int eventID, Object parameter) {
        super(caller, eventID, parameter);
        this.browser = browser;
    }

    public ExtBrowser getBrowser() {
        return browser;
    }

}

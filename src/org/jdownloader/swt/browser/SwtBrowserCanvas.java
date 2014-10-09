package org.jdownloader.swt.browser;

import java.awt.Canvas;

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Shell;

public abstract class SwtBrowserCanvas extends Canvas {
    private Browser browser;

    public Browser getBrowser() {
        return browser;
    }

    abstract public void connect();

    public void setBrowser(Browser browser) {
        this.browser = browser;
    }

    private Shell shell;

    public Shell getShell() {
        return shell;
    }

    public void setShell(Shell shell) {
        if (this.shell != null) {
            throw new IllegalStateException("Shell already Set");
        }
        this.shell = shell;
    }

}

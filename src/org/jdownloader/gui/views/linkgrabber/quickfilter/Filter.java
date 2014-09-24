package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.Icon;

import jd.controlling.faviconcontroller.FavIconRequestor;
import jd.controlling.linkcrawler.CrawledLink;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.jdownloader.images.NewTheme;

public abstract class Filter implements FavIconRequestor {

    protected Icon                 icon    = null;
    protected AtomicInteger        counter = new AtomicInteger(0);
    protected boolean              enabled = false;
    protected static final Storage CONFIG  = JSonStorage.getPlainStorage("quickfilters");
    static {
        CONFIG.setAutoPutValues(false);
    }

    public int getCounter() {
        return counter.get();
    }

    public void resetCounter() {
        counter.set(0);
    }

    public void setCounter(int i) {
        counter.set(i);
    }

    public void increaseCounter() {
        counter.incrementAndGet();
    }

    public Icon getIcon() {
        return icon;
    }

    public void setIcon(Icon icon) {
        if (icon != null) {
            this.icon = NewTheme.I().getScaledInstance(icon, 16);
        }
    }

    protected Filter(String string) {
        this.name = string;
    }

    public Filter(String string, Icon icon) {
        this.name = string;
        if (icon != null) {
            this.icon = NewTheme.I().getScaledInstance(icon, 16);
        }
        enabled = Boolean.TRUE.equals(CONFIG.get(getID(), true));
    }

    abstract protected String getID();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (!enabled) {
            CONFIG.put(getID(), false);
        } else {
            CONFIG.remove(getID());
        }
    }

    public String getName() {
        return name;
    }

    protected final String name;

    public Icon setFavIcon(Icon icon) {
        setIcon(icon);
        return icon;
    }

    abstract public boolean isFiltered(CrawledLink link);

    public String getDescription() {
        return null;
    }
}

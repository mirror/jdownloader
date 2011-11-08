package org.jdownloader.gui.views.linkgrabber.quickfilter;

import javax.swing.ImageIcon;

import jd.controlling.FavIconRequestor;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.jdownloader.images.NewTheme;

public abstract class Filter<E extends AbstractPackageNode<V, E>, V extends AbstractPackageChildrenNode<E>> implements FavIconRequestor {

    private ImageIcon      icon    = null;
    protected int          counter = 0;
    private FilterSettings config;

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public ImageIcon getIcon() {
        return icon;
    }

    public void setIcon(ImageIcon icon) {
        if (icon != null) this.icon = NewTheme.I().getScaledInstance(icon, 16);
    }

    public Filter(String string, ImageIcon icon) {
        this.name = string;
        if (icon != null) this.icon = NewTheme.I().getScaledInstance(icon, 16);
        config = JsonConfig.create(Application.getResource("cfg/quickfilter_" + getID()), FilterSettings.class);

    }

    abstract protected String getID();

    public boolean isEnabled() {
        return config.isEnabled();
    }

    public void setEnabled(boolean enabled) {

        config.setEnabled(enabled);
    }

    public String getName() {
        return name;
    }

    public void setName(String hoster) {
        this.name = hoster;
    }

    protected String name = null;

    public ImageIcon setFavIcon(ImageIcon icon) {
        setIcon(icon);
        return icon;
    }

    abstract public boolean isFiltered(V link);

    abstract public boolean isFiltered(E link);

    public String getDescription() {
        return null;
    }
}

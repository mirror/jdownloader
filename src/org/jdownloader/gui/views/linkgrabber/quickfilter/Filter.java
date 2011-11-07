package org.jdownloader.gui.views.linkgrabber.quickfilter;

import javax.swing.ImageIcon;

import jd.controlling.FavIconRequestor;
import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.jdownloader.images.NewTheme;

public abstract class Filter<E extends AbstractPackageNode<V, E>, V extends AbstractPackageChildrenNode<E>> implements FavIconRequestor {
    protected boolean enabled = true;
    private ImageIcon icon    = null;
    protected int     counter = 0;

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

    public Filter(String string, ImageIcon icon, boolean b) {
        this.name = string;
        if (icon != null) this.icon = NewTheme.I().getScaledInstance(icon, 16);
        this.enabled = b;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = !enabled;
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

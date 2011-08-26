package org.jdownloader.gui.views.linkgrabber;

import javax.swing.ImageIcon;

import jd.controlling.FavIconRequestor;

import org.jdownloader.images.NewTheme;

public class Filter implements FavIconRequestor {
    private boolean   enabled = true;
    private ImageIcon icon    = null;
    private String    info    = "" + (int) (Math.random() * 100);

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public ImageIcon getIcon() {
        return icon;
    }

    public void setIcon(ImageIcon icon) {
        if (icon != null) this.icon = NewTheme.I().getScaledInstance(icon, 16);
    }

    public Filter(String string, ImageIcon icon, boolean b) {
        this.hoster = string;
        if (icon != null) this.icon = NewTheme.I().getScaledInstance(icon, 16);
        this.enabled = b;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHoster() {
        return hoster;
    }

    public void setHoster(String hoster) {
        this.hoster = hoster;
    }

    private String hoster = null;

    public void setFavIcon(ImageIcon icon) {
        setIcon(icon);
    }
}

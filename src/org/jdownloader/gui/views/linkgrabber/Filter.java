package org.jdownloader.gui.views.linkgrabber;

import javax.swing.ImageIcon;

import org.jdownloader.images.NewTheme;

public class Filter {
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
        this.icon = icon;
    }

    public Filter(String string, ImageIcon icon, boolean b) {
        this.hoster = string;
        this.icon = NewTheme.I().getScaledInstance(icon, 16);
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
}

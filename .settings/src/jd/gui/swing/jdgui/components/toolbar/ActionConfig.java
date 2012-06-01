package jd.gui.swing.jdgui.components.toolbar;

import org.appwork.storage.Storable;

public class ActionConfig implements Storable {
    public ActionConfig(/* STorable */) {

    }

    public String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean visible;
}

package org.jdownloader.gui.views.downloads.contextmenumanager;

import org.appwork.storage.Storable;

public class MenuContainerRoot extends MenuContainer implements Storable {
    private int version;

    public MenuContainerRoot(/* Storable */) {

    }

    public void setSource(int version) {
        this.version = version;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

}

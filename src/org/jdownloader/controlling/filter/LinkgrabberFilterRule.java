package org.jdownloader.controlling.filter;

import org.appwork.storage.Storable;

public class LinkgrabberFilterRule extends FilterRule implements Storable {

    public LinkgrabberFilterRule() {
        // required by Storable
    }

    private boolean accept;

    public void setAccept(boolean b) {
        accept = b;
    }

    public boolean isAccept() {
        return accept;
    }

}

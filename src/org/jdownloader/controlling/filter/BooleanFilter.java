package org.jdownloader.controlling.filter;

import org.appwork.storage.Storable;
import org.jdownloader.gui.translate._GUI;

public class BooleanFilter extends Filter implements Storable {

    public BooleanFilter() {
        // required by Storable
    }

    public BooleanFilter(boolean selected) {
        setEnabled(selected);
    }

    public String toString() {
        return _GUI._.BooleanFilter_toString_();
    }

}

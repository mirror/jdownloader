package org.jdownloader.controlling.filter;

import org.jdownloader.gui.translate._GUI;

public class BooleanFilter extends Filter {

    public BooleanFilter(boolean selected) {
        setEnabled(selected);
    }

    public String toString() {
        return _GUI._.BooleanFilter_toString_();
    }

}

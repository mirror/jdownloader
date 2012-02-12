package org.jdownloader.gui.views.components;

import javax.swing.ImageIcon;

import org.jdownloader.gui.translate._GUI;

public enum SearchCategory {

    FILENAME(_GUI._.searchcategory_filename()),
    HOSTER(_GUI._.searchcategory_hoster()),
    PACKAGE(_GUI._.searchcategory_package());

    private String label;

    private SearchCategory(String searchcategory_filename) {
        label = searchcategory_filename;
    }

    public String getLabel() {
        return label;
    }

    public ImageIcon getIcon() {
        return null;
    }

}

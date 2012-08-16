package org.jdownloader.extensions.streaming.gui.sidebar;

import javax.swing.DefaultListModel;

import org.jdownloader.extensions.streaming.mediaarchive.categories.AudioRootCategory;
import org.jdownloader.extensions.streaming.mediaarchive.categories.ImagesRootCategory;
import org.jdownloader.extensions.streaming.mediaarchive.categories.VideoRootCategory;

public class SettingsSidebarModel extends DefaultListModel {

    private static final long serialVersionUID = -204494527404304349L;
    private Object            LOCK             = new Object();

    public SettingsSidebarModel() {
        super();

        addElement(new VideoRootCategory());
        addElement(new AudioRootCategory());
        addElement(new ImagesRootCategory());

    }

}

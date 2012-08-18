package org.jdownloader.extensions.streaming.gui.categories;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.T;

public class AudioRootCategory extends RootCategory {

    public AudioRootCategory(StreamingExtension plg) {
        super(plg, T._.SettingsSidebarModel_audio(), "audio");
    }

    @Override
    public JComponent getView() {
        return new JLabel("TODO");

    }

}

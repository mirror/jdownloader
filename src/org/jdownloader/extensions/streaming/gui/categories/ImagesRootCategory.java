package org.jdownloader.extensions.streaming.gui.categories;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.T;

public class ImagesRootCategory extends RootCategory {

    public ImagesRootCategory(StreamingExtension plg) {
        super(plg, T._.SettingsSidebarModel_images(), "image");
    }

    @Override
    public JComponent getView() {
        return new JLabel("TODO");
    }

}

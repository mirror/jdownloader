package org.jdownloader.extensions.streaming.gui.categories;

import javax.swing.JComponent;

import org.jdownloader.extensions.streaming.StreamingExtension;
import org.jdownloader.extensions.streaming.T;
import org.jdownloader.extensions.streaming.gui.image.ImageTable;
import org.jdownloader.extensions.streaming.gui.image.ImageTableModel;

public class ImagesRootCategory extends RootCategory {

    private ImageTableModel model;
    private ImageTable      table;

    public ImagesRootCategory(StreamingExtension plg) {
        super(plg, T._.SettingsSidebarModel_images(), "image");
        model = new ImageTableModel(plg, plg.getMediaArchiveController());
        table = new ImageTable(model);
    }

    @Override
    public JComponent getView() {
        return table;
    }

}

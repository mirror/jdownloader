package org.jdownloader.extensions.infobar;

import javax.swing.JLabel;

import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.updatev2.gui.LAFOptions;

import jd.gui.swing.jdgui.views.settings.panels.advanced.AdvancedConfigTableModel;
import jd.gui.swing.jdgui.views.settings.panels.advanced.AdvancedTable;

public class InfoBarConfigPanel extends ExtensionConfigPanel<InfoBarExtension> {

    /**
     *
     */
    private static final long        serialVersionUID = 1L;
    private AdvancedConfigTableModel model;

    public InfoBarConfigPanel(InfoBarExtension extension) {
        super(extension);
        JLabel lbl;
        add(SwingUtils.toBold(lbl = new JLabel("THIS EXTENSION IS STILL UNDER CONSTRUCTION. Feel free to test it and to give Feedback.")));
        lbl.setForeground(LAFOptions.getInstance().getColorForErrorForeground());
        add(new AdvancedTable(model = new AdvancedConfigTableModel("InfoBarConfigPanel") {
            @Override
            public void refresh(String filterText) {
                _fireTableStructureChanged(register(), true);
            }
        }));
        model.refresh("InfoBar");

    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {

    }

}

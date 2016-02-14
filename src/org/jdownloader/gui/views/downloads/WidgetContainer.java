package org.jdownloader.gui.views.downloads;

import javax.swing.JComponent;

import org.appwork.swing.MigPanel;
import org.jdownloader.gui.views.components.HeaderScrollPane;
import org.jdownloader.updatev2.gui.LAFOptions;

public class WidgetContainer extends MigPanel {
    public WidgetContainer() {
        setOpaque(false);

    }

    public void relayout() {
    }

    protected JComponent wrap(HeaderScrollPane panel) {
        return LAFOptions.getInstance().getExtension().customizeLayoutWrapTitledPanels(panel);
    }

    public void refreshAfterTabSwitch() {
    }

}

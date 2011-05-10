package jd.gui.swing.jdgui.views.settings.sidebar;

import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;

@SuppressWarnings("rawtypes")
public class EmptyExtensionConfigPanel extends ExtensionConfigPanel {

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    public EmptyExtensionConfigPanel(AbstractExtension ext) {
        super(ext, false);

    }

    @Override
    protected void onShow() {
    }

    @Override
    protected void onHide() {
    }

}
package jd.gui.swing.jdgui.views.settings.components.LinkgrabberFilter;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.jdownloader.gui.translate.T;
import org.jdownloader.images.Theme;

public class RemoveAction extends AbstractAction {
    private FilterTable table;

    public RemoveAction(FilterTable table) {
        this.table = table;
        this.putValue(NAME, T._.settings_linkgrabber_filter_action_remove());
        this.putValue(AbstractAction.SMALL_ICON, Theme.getIcon("remove", 20));
    }

    @Override
    public boolean isEnabled() {
        return table.getSelectedRows().length > 0;
    }

    public void actionPerformed(ActionEvent e) {
    }

}

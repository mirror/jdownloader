package jd.gui.swing.jdgui.views.settings.components.LinkgrabberFilter;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.jdownloader.gui.translate.T;
import org.jdownloader.images.Theme;

public class EnableAction extends AbstractAction {
    private FilterTable table;

    public EnableAction(FilterTable table) {
        this.table = table;
        this.putValue(NAME, table.getExtTableModel().getSelectedObjects().size() == 0 ? T._.settings_linkgrabber_filter_action_enable_all() : T._.settings_linkgrabber_filter_action_enable());
        this.putValue(AbstractAction.SMALL_ICON, Theme.getIcon("enable", 20));
    }

    @Override
    public boolean isEnabled() {
        if (table.getExtTableModel().getSelectedObjects().size() == 0) return true;
        for (LinkFilter f : table.getExtTableModel().getSelectedObjects()) {
            if (!f.isEnabled()) return true;
        }
        return false;
    }

    public void actionPerformed(ActionEvent e) {
    }

}

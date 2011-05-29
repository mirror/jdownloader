package jd.gui.swing.jdgui.views.settings.components.LinkgrabberFilter;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class DisableAction extends AbstractAction {
    private static final long serialVersionUID = 8468803176566988760L;
    private FilterTable       table;

    public DisableAction(FilterTable table) {
        this.table = table;
        this.putValue(NAME, table.getExtTableModel().getSelectedObjects().size() == 0 ? _GUI._.settings_linkgrabber_filter_action_all() : _GUI._.settings_linkgrabber_filter_action_disable());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("disable", 20));
    }

    @Override
    public boolean isEnabled() {
        if (table.getExtTableModel().getSelectedObjects().size() == 0) return true;
        for (LinkFilter f : table.getExtTableModel().getSelectedObjects()) {
            if (f.isEnabled()) return true;
        }
        return false;
    }

    public void actionPerformed(ActionEvent e) {
    }

}

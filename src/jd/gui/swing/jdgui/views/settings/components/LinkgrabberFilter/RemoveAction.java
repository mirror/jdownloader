package jd.gui.swing.jdgui.views.settings.components.LinkgrabberFilter;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;

import org.appwork.utils.swing.table.utils.MinimumSelectionObserver;
import org.jdownloader.gui.translate.T;
import org.jdownloader.images.Theme;

public class RemoveAction extends AbstractAction {
    private FilterTable table;

    public RemoveAction(FilterTable table) {
        this.table = table;
        this.putValue(NAME, T._.settings_linkgrabber_filter_action_remove());
        this.putValue(AbstractAction.SMALL_ICON, Theme.getIcon("remove", 20));
        table.getSelectionModel().addListSelectionListener(new MinimumSelectionObserver(table, this, 1));

    }

    public RemoveAction(ArrayList<LinkFilter> selected) {

        this.putValue(NAME, T._.settings_linkgrabber_filter_action_remove());
        this.putValue(AbstractAction.SMALL_ICON, Theme.getIcon("remove", 20));

        setEnabled(selected.size() > 0);
    }

    public void actionPerformed(ActionEvent e) {
    }

}

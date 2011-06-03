package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;

import org.appwork.utils.swing.table.utils.MinimumSelectionObserver;
import org.jdownloader.controlling.LinkFilter;
import org.jdownloader.controlling.LinkFilterController;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class RemoveAction extends AbstractAction {
    private static final long     serialVersionUID = -477419276505058907L;
    private LinkgrabberFilter     filter;
    private ArrayList<LinkFilter> selected;

    public RemoveAction(LinkgrabberFilter filter) {
        this.filter = filter;
        this.putValue(NAME, _GUI._.settings_linkgrabber_filter_action_remove());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("remove", 20));
        filter.getTable().getSelectionModel().addListSelectionListener(new MinimumSelectionObserver(filter.getTable(), this, 1));

    }

    public RemoveAction(ArrayList<LinkFilter> selected) {
        this.selected = selected;
        this.putValue(NAME, _GUI._.settings_linkgrabber_filter_action_remove());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("remove", 20));

        setEnabled(selected.size() > 0);
    }

    public void actionPerformed(ActionEvent e) {
        ArrayList<LinkFilter> remove = selected;
        if (remove == null) {
            remove = filter.getTable().getExtTableModel().getSelectedObjects();
        }
        for (LinkFilter lf : remove) {
            LinkFilterController.getInstance().remove(lf);
        }

        ((FilterTableModel) filter.getTable().getExtTableModel()).fill();
    }

}

package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.controlling.IOEQ;

import org.jdownloader.controlling.filter.FilterRule;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class NewAction extends AbstractAction {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private FilterTable       table;

    public NewAction(LinkgrabberFilter linkgrabberFilter) {
        this(linkgrabberFilter.getTable());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("add", 20));

    }

    public NewAction(FilterTable table) {
        this.table = table;
        this.putValue(NAME, _GUI._.settings_linkgrabber_filter_action_add());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("add", 16));
    }

    public void actionPerformed(ActionEvent e) {
        IOEQ.add(new Runnable() {

            public void run() {
                LinkFilterController.getInstance().add(new FilterRule());
                table.getExtTableModel()._fireTableStructureChanged(LinkFilterController.getInstance().list(), false);
            }

        }, true);

    }

}

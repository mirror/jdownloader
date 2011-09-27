package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.controlling.IOEQ;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.FilterRuleDialog;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.controlling.filter.LinkgrabberFilterRule;
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
        final LinkgrabberFilterRule rule = new LinkgrabberFilterRule();
        FilterRuleDialog d = new FilterRuleDialog(rule);
        try {
            Dialog.getInstance().showDialog(d);
            rule.setEnabled(true);
            IOEQ.add(new Runnable() {

                public void run() {

                    LinkFilterController.getInstance().add(rule);
                    table.getExtTableModel()._fireTableStructureChanged(LinkFilterController.getInstance().list(), false);
                }

            }, true);
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }

}

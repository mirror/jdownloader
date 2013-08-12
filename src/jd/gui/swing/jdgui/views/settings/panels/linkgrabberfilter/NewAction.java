package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import jd.controlling.TaskQueue;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.ConditionDialog;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.ExceptionsRuleDialog;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.FilterRuleDialog;

import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.controlling.filter.LinkgrabberFilterRule;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.AbstractAddAction;
import org.jdownloader.images.NewTheme;

public class NewAction extends AbstractAddAction {
    /**
     * 
     */
    private static final long   serialVersionUID = 1L;

    private AbstractFilterTable table;

    private LinkgrabberFilter   linkgrabberFilter;

    public NewAction(LinkgrabberFilter linkgrabberFilter) {
        this.linkgrabberFilter = linkgrabberFilter;
        this.putValue(NAME, _GUI._.settings_linkgrabber_filter_action_add());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("add", 20));

    }

    public NewAction(AbstractFilterTable table) {
        this.table = table;
        this.putValue(NAME, _GUI._.settings_linkgrabber_filter_action_add());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("add", 16));
    }

    public void actionPerformed(ActionEvent e) {
        final LinkgrabberFilterRule rule = new LinkgrabberFilterRule();
        add(rule, getTable());
    }

    private AbstractFilterTable getTable() {
        if (table != null) return table;
        return linkgrabberFilter.getTable();
    }

    public static void add(final LinkgrabberFilterRule rule, final AbstractFilterTable table) {
        ConditionDialog<LinkgrabberFilterRule> d;
        if (table instanceof FilterTable) {
            d = new FilterRuleDialog(rule);
        } else {
            d = new ExceptionsRuleDialog(rule);
        }

        try {
            Dialog.getInstance().showDialog(d);
            rule.setEnabled(true);
            TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    LinkFilterController.getInstance().add(rule);
                    return null;
                }
            });
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }
}

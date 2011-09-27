package jd.gui.swing.jdgui.views.settings.panels.packagizer;

import java.awt.event.ActionEvent;

import jd.controlling.IOEQ;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.controlling.packagizer.PackagizerRule;
import org.jdownloader.gui.views.components.AbstractAddAction;

public class NewAction extends AbstractAddAction {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private FilterTable       table;

    public NewAction(FilterTable table) {
        super();
        this.table = table;

    }

    public void actionPerformed(ActionEvent e) {
        final PackagizerRule rule = new PackagizerRule();
        PackagizerFilterRuleDialog d = new PackagizerFilterRuleDialog(rule);
        try {
            Dialog.getInstance().showDialog(d);
            rule.setEnabled(true);
            IOEQ.add(new Runnable() {

                public void run() {

                    PackagizerController.getInstance().add(rule);
                    table.getExtTableModel()._fireTableStructureChanged(PackagizerController.getInstance().list(), false);
                }

            }, true);
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }

}
